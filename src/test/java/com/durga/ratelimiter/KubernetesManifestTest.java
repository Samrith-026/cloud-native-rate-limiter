package com.durga.ratelimiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class KubernetesManifestTest {
    @Test
    void everyDocumentHasKubernetesTypeMetadata() throws Exception {
        var manifests = Files.list(Path.of("k8s"))
                .filter(path -> path.toString().endsWith(".yaml"))
                .sorted()
                .toList();
        assertThat(manifests).isNotEmpty();
        int documents = 0;
        for (Path manifest : manifests) {
            try (Reader reader = Files.newBufferedReader(manifest)) {
                for (Object document : new Yaml().loadAll(reader)) {
                    assertThat(document).as(manifest.toString()).isInstanceOf(Map.class);
                    Map<?, ?> resource = (Map<?, ?>) document;
                    assertTrue(resource.containsKey("apiVersion"), manifest + " is missing apiVersion");
                    assertTrue(resource.containsKey("kind"), manifest + " is missing kind");
                    assertTrue(resource.containsKey("metadata"), manifest + " is missing metadata");
                    documents++;
                }
            }
        }
        assertThat(documents).isEqualTo(11);
    }
}
