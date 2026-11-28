package com.ficn.panel.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ficn.panel.client.clickhouse.ClickHouseClient;
import com.ficn.panel.model.entity.vo.AuditAlertPageVO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditRepositorySearchTest {

    private final FakeClickHouseClient client = new FakeClickHouseClient();
    private final AuditRepository repository = new AuditRepository(client, new ObjectMapper());

    @Test
    void countsEntireRangeAndReturnsOnlyRequestedPage() {
        String first = "00000000-0000-0000-0000-000000000003";
        String second = "00000000-0000-0000-0000-000000000002";
        String time = "2026-11-28 10:00:00.123456789";
        client.rows = List.of(row(first, time), row(second, time));
        client.count = 23;
        client.redCount = 5;
        client.orangeCount = 7;

        AuditAlertPageVO page = repository.searchAlerts(null, null, null, 1000, 2000, 2, 10, true);

        assertThat(page.total()).isEqualTo(23);
        assertThat(page.redCount()).isEqualTo(5);
        assertThat(page.orangeCount()).isEqualTo(7);
        assertThat(page.blueCount()).isEqualTo(11);
        assertThat(page.items()).extracting(alert -> alert.getUuid()).containsExactly(first, second);
        assertThat(client.queryCount).isEqualTo(2);
        assertThat(client.countSql).contains("SELECT count() AS cnt")
                .contains("countIf(toString(priority) IN")
                .contains("time >= fromUnixTimestamp64Milli({startMs:Int64})")
                .contains("time < fromUnixTimestamp64Milli({endMs:Int64})");
        assertThat(client.sql).contains("ORDER BY time DESC, uuid DESC")
                .contains("OFFSET {offset:UInt32}");
        assertThat(client.params).containsEntry("limit", 2)
                .containsEntry("offset", 10)
                .containsEntry("startMs", 1000L)
                .containsEntry("endMs", 2000L);
    }

    @Test
    void appliesSameFiltersToCountAndPage() {
        client.count = 1;
        repository.searchAlerts("rule", "namespace", "Warning", 1000, 2000, 10, 0, true);

        assertThat(client.countSql).contains("rule = {rule:String}")
                .contains("namespace = {namespace:String}")
                .contains("toString(priority) = {priority:String}");
        assertThat(client.sql).contains("ORDER BY time DESC, uuid DESC")
                .contains("rule = {rule:String}")
                .contains("namespace = {namespace:String}");
        assertThat(client.params).containsEntry("rule", "rule")
                .containsEntry("namespace", "namespace")
                .containsEntry("priority", "Warning");
    }

    @Test
    void returnsTotalAndEmptyPageWhenOffsetIsPastEnd() {
        client.count = 3;
        AuditAlertPageVO page = repository.searchAlerts(null, null, null, 1000, 2000, 10, 10, true);
        assertThat(page.total()).isEqualTo(3);
        assertThat(page.items()).isEmpty();
        assertThat(client.queryCount).isEqualTo(1);
        assertThat(client.sql).isNull();
    }

    @Test
    void pageOnlyQuerySkipsSummaryAndKeepsAllFilters() {
        client.rows = List.of(row("00000000-0000-0000-0000-000000000004",
                "2026-11-28 10:00:00.123456789"));

        AuditAlertPageVO page = repository.searchAlerts(
                "rule", "namespace", "Warning", 1000, 2000, 10, 20, false);

        assertThat(page.total()).isNull();
        assertThat(page.redCount()).isNull();
        assertThat(page.orangeCount()).isNull();
        assertThat(page.blueCount()).isNull();
        assertThat(page.items()).hasSize(1);
        assertThat(client.queryCount).isEqualTo(1);
        assertThat(client.countSql).isNull();
        assertThat(client.sql).contains("time >= fromUnixTimestamp64Milli({startMs:Int64})")
                .contains("time < fromUnixTimestamp64Milli({endMs:Int64})")
                .contains("rule = {rule:String}")
                .contains("namespace = {namespace:String}")
                .contains("toString(priority) = {priority:String}")
                .contains("ORDER BY time DESC, uuid DESC LIMIT {limit:UInt32} OFFSET {offset:UInt32}");
        assertThat(client.params).containsEntry("startMs", 1000L)
                .containsEntry("endMs", 2000L)
                .containsEntry("rule", "rule")
                .containsEntry("namespace", "namespace")
                .containsEntry("priority", "Warning")
                .containsEntry("limit", 10)
                .containsEntry("offset", 20);
    }

    @Test
    void periodReportAggregatesAllAlertsAndFetchesOnlyRedDetails() {
        client.periodRows = true;

        AuditRepository.PeriodReportData data = repository.periodReportData(1000, 2000);

        assertThat(data.priorities()).containsExactly(
                new AuditRepository.PriorityCount("Critical", "严重", 1));
        assertThat(data.rules()).containsExactly(
                new AuditRepository.RuleCount("Create Pod", "创建容器", 3));
        assertThat(data.redAlerts()).extracting(alert -> alert.getUuid())
                .containsExactly("00000000-0000-0000-0000-000000000001");
        assertThat(client.sql).contains("toString(priority) IN ('Emergency', 'Alert', 'Critical', 'Error')")
                .contains("ORDER BY time DESC, uuid DESC");
        assertThat(client.params).containsEntry("startMs", 1000L).containsEntry("endMs", 2000L);
    }

    private static Map<String, Object> row(String uuid, String time) {
        return Map.of("uuid", uuid, "time_raw", time);
    }

    private static class FakeClickHouseClient extends ClickHouseClient {
        private List<Map<String, Object>> rows = List.of();
        private String sql;
        private String countSql;
        private Map<String, Object> params;
        private boolean periodRows;
        private long count;
        private long redCount;
        private long orangeCount;
        private int queryCount;

        private FakeClickHouseClient() {
            super(null, null, null);
        }

        @Override
        public List<Map<String, Object>> query(String sql, Map<String, Object> params) {
            queryCount++;
            this.params = params;
            if (periodRows) {
                this.sql = sql;
                if (sql.startsWith("SELECT toString(priority)")) {
                    return List.of(Map.of("priority", "Critical", "priority_cn", "严重", "cnt", 1));
                }
                if (sql.startsWith("SELECT rule, rule_cn")) {
                    return List.of(Map.of("rule", "Create Pod", "rule_cn", "创建容器", "cnt", 3));
                }
                return List.of(row("00000000-0000-0000-0000-000000000001",
                        "2026-11-28 10:00:00.123456789"));
            }
            if (sql.startsWith("SELECT count() AS cnt")) {
                this.countSql = sql;
                return List.of(Map.of("cnt", count, "red_count", redCount,
                        "orange_count", orangeCount));
            }
            this.sql = sql;
            return rows;
        }
    }
}
