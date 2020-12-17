package net.kwonoh.gephi.cli.command;

import org.gephi.appearance.api.AppearanceController;
import org.gephi.appearance.api.AppearanceModel;
import org.gephi.appearance.api.Function;
import org.gephi.appearance.plugin.RankingElementColorTransformer;
import org.gephi.appearance.plugin.RankingNodeSizeTransformer;
import org.gephi.appearance.plugin.UniqueLabelSizeTransformer;
import org.gephi.appearance.spi.Transformer;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.EdgeDirectionDefault;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.fruchterman.FruchtermanReingold;
import org.gephi.layout.plugin.labelAdjust.LabelAdjust;
import org.gephi.layout.spi.Layout;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperties;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Level;


@Command(name = "custom-1",
        sortOptions = false,
        mixinStandardHelpOptions = true,
        version = "custom-1 0.1")
public class Custom1 extends AbstractGephiCommand implements Callable<Void> {
    private static final java.util.logging.Logger log =
            java.util.logging.Logger.getLogger(Custom1.class.getName());

    @Option(names = {"-i", "--in"},
            paramLabel = "FILE",
            required = true,
            description = "The input file path (gml).")
    protected String inputFilePath;

    @Option(names = {"-o", "--out"},
            paramLabel = "FILE",
            required = true,
            description = "The output file path (PDF or SVG).")
    protected String outputFilePath;

    @Option(names = "--max-iters",
            paramLabel = "INT",
            description = "The number of maximum iterations for layout computation (default: 500)")
    protected int maxIters = 5000;

    @Option(names = "--node-min-size",
            paramLabel = "INT",
            description = "The minimum node size (default: 1)")
    protected int minNodeSize = 1;

    @Option(names = "--node-max-size",
            paramLabel = "INT",
            description = "The maximum node size (default: 50)")
    protected int maxNodeSize = 50;

    @Option(names = "--label-size",
            paramLabel = "NUM",
            description = "Scale factor of label size (default: 2.0)")
    protected float labelSize = 2f;

    @Option(names = "--colour-left",
            paramLabel = "STRING",
            description = "Colour of nodes on the left side of the gradient (default: EDF8FB (polar))")
    protected String colourLeft = "EDF8FB";

    @Option(names = "--colour-mid",
            paramLabel = "STRING",
            description = "Colour of nodes on the middle of the gradient (default: 66C2A4 (tradewind))")
    protected String colourMid = "66C2A4";

    @Option(names = "--colour-right",
            paramLabel = "STRING",
            description = "Colour of nodes on the right side of the gradient (default: 006D2C (fun green))")
    protected String colourRight = "006D2C";

    @Override
    public Void call() {
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        Workspace workspace = pc.getCurrentWorkspace();

        //Get models and controllers for this new workspace - will be useful later
        GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
        PreviewModel model = Lookup.getDefault().lookup(PreviewController.class).getModel();
        ImportController importController = Lookup.getDefault().lookup(ImportController.class);
        AppearanceController appearanceController = Lookup.getDefault().lookup(AppearanceController.class);

        //Import file
        Container container;
        try {
            File file = new File(inputFilePath);
            container = importController.importFile(file);
            container.getLoader().setEdgeDefault(EdgeDirectionDefault.UNDIRECTED);
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Cannot read input graph", ex);
            return null;
        }

        //Append imported data to GraphAPI
        importController.process(container, new DefaultProcessor(), workspace);

        //See if graph is well imported
        Graph graph = graphModel.getGraph();
        log.log(Level.INFO, "Read {0} nodes and {1} edges", new Object[] { graph.getNodeCount(), graph.getEdgeCount() });

        // Patch the layout a bit -- very hacky
        AppearanceModel m = appearanceController.getModel();
        Function[] funcs = m.getNodeFunctions(graph);

        // Note: order is important here!
        TransformerConfiguration<?>[] configs = new TransformerConfiguration[] {
                new TransformerConfiguration<>(
                        RankingNodeSizeTransformer.class,
                        t -> {
                            t.setMinSize(minNodeSize);
                            t.setMaxSize(maxNodeSize);
                        }
                ),
                new TransformerConfiguration<>(
                        RankingElementColorTransformer.class,
                        t -> t.setColors(new Color[] {
                            new Color(Integer.parseInt(colourLeft, 16)),
                            new Color(Integer.parseInt(colourMid, 16)),
                            new Color(Integer.parseInt(colourRight, 16)),
                        })
                ),
                new TransformerConfiguration<>(
                        UniqueLabelSizeTransformer.class,
                        t -> t.setSize(labelSize)
                )
        };

        // Apply al configs in the defined order
        for(TransformerConfiguration<?> tc : configs) {
            tc.apply(funcs, appearanceController::transform);
        }

        // Layouting
        Layout[] layouts = {
                new FruchtermanReingold(null),
                new LabelAdjust(null)
        };
        for(Layout l : layouts) {
            l.setGraphModel(graphModel);
            l.resetPropertiesValues();
            l.initAlgo();
            for (int i = 0; i < 5000 && l.canAlgo(); i++) {
                l.goAlgo();
            }
            l.endAlgo();
        }

        // Configure preview settings (i.e. defining what the exported file looks like)
        PreviewProperties properties = model.getProperties();
        Pair<String, Object>[] props = new Pair[]{
                new Pair<>(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE),
                new Pair<>(PreviewProperty.EDGE_COLOR, new EdgeColor(Color.GRAY)),
                new Pair<>(PreviewProperty.EDGE_THICKNESS, 0.1f),
                new Pair<>(PreviewProperty.NODE_LABEL_FONT, properties.getFontValue(PreviewProperty.NODE_LABEL_FONT).deriveFont(8f)),
                new Pair<>(PreviewProperty.NODE_LABEL_PROPORTIONAL_SIZE, Boolean.FALSE)
        };

        for(Pair<String, Object> p : props) {
            properties.putValue(p.fst, p.snd);
        }

        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        File out = new File(outputFilePath);
        try {
            ec.exportFile(out);
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Cannot write output file", ex);
        }

        log.log(Level.INFO, "Done. Result written to {0}", out.getPath());
        return null;
    }

    static class TransformerConfiguration<T extends Transformer> {
        private final Class<T> transformerClass;
        private final Consumer<T> applicator;
        public TransformerConfiguration(Class<T> transformerClass, Consumer<T> applicator) {
            this.transformerClass = transformerClass;
            this.applicator = applicator;
        }
        public void apply(Function[] functions, Consumer<Function> effectuate) {
            for (Function f : functions) {
                Transformer t = f.getTransformer();
                if(transformerClass == t.getClass()) {
                    log.log(Level.INFO, "Applying transformer {0}", t.getClass().getSimpleName());
                    applicator.accept((T) t);
                    effectuate.accept(f);
                }
            }
        }
    }

    private static class Pair<A, B> {
        public final A fst;
        public final B snd;
        public Pair(A fst, B snd) {
            this.fst = fst;
            this.snd = snd;
        }
    }
}
