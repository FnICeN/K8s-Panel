package com.ficn.panel.client.k8s;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PodApiTest {

    @Test
    void getLogMakesOneDirectKubernetesRequest() {
        K8sHttpClient client = mock(K8sHttpClient.class);
        String path = "/api/v1/namespaces/demo/pods/web-1/log?container=app&tailLines=500";
        when(client.getText("token", path)).thenReturn("line one");

        String logs = new PodApi(client).getLog("token", "demo", "web-1", "app", 500);

        assertThat(logs).isEqualTo("line one");
        verify(client, only()).getText("token", path);
    }
}
