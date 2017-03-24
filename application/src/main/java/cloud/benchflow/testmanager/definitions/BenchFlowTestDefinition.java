package cloud.benchflow.testmanager.definitions;

import org.yaml.snakeyaml.Yaml;

import java.util.Map;

/**
 * @author Jesper Findahl (jesper.findahl@usi.ch)
 *         created on 13.02.17.
 */
public class BenchFlowTestDefinition {

    private final String definition;

    public BenchFlowTestDefinition(String definition) {
        this.definition = definition;
    }

    public String getName() {

        // TODO - parse using DSL

        Map<String, Object> parsedExpConfig = (Map<String, Object>) new Yaml().load(definition);

        return (String) parsedExpConfig.get("testName");

    }
}