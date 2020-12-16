package net.kwonoh.gephi.cli.command;

import org.gephi.graph.api.GraphModel;
import org.gephi.layout.plugin.fruchterman.FruchtermanReingold;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;


@Command(name = "fruchterman-reingold",
        sortOptions = false,
        mixinStandardHelpOptions = true,
        version = "fruchterman-reingold 0.1")
public class FruchtermanReingoldLayout extends AbstractGephiCommand implements Callable<Void> {
    @Option(names = {"-i", "--in"},
            paramLabel = "FILE",
            required = true,
            description = "The input file path.")
    protected String inputFilePath;

    @Option(names = {"-o", "--out"},
            paramLabel = "FILE",
            required = true,
            description = "The output file path.")
    protected String outputFilePath;

    @Option(names = "--max-iters",
            paramLabel = "INT",
            description = "The number of maximum iterations for layout computation (default: 100)")
    protected int maxIters = 100;

    @Option(names = "--area",
            paramLabel = "NUM",
            description = "Area parameter (default: 10000.0).")
    protected Float area;

    @Option(names = "--gravity",
            paramLabel = "NUM",
            description = "Gravity parameter (default: 10.00).")
    protected Double gravity = null;

    @Option(names = "--speed",
            paramLabel = "NUM",
            description = "Speed parameter (default: 1.00).")
    protected Double speed = null;

    @Override
    public Void call() throws Exception {
        GraphModel graphModel = importGraph(inputFilePath);
        computeLayout(graphModel);
        exportGraph(outputFilePath);
        return null;
    }

    protected void computeLayout(GraphModel graphModel) {
        FruchtermanReingold layout = new FruchtermanReingold(null);
        layout.setGraphModel(graphModel);
        setLayoutProperties(layout);

        layout.initAlgo();
        for (int i = 0; i < maxIters && layout.canAlgo(); i++) {
            layout.goAlgo();
        }
        layout.endAlgo();
    }

    protected void setLayoutProperties(FruchtermanReingold layout) {
        layout.resetPropertiesValues();

        if(area != null) {
            layout.setArea(area);
        }

        if(gravity != null) {
            layout.setGravity(gravity);
        }

        if(speed != null) {
            layout.setSpeed(speed);
        }
    }
}
