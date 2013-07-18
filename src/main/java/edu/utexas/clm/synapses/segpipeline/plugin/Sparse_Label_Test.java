package edu.utexas.clm.synapses.segpipeline.plugin;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabelFactory;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ShortProcessor;

import java.util.ArrayList;

/**
 *
 */
public class Sparse_Label_Test implements PlugIn
{
    public void run(String s)
    {
        ImagePlus imp = IJ.getImage();
        if (imp != null)
        {
            IJ.log("Creating factory");
            SparseLabelFactory factory = new SparseLabelFactory(imp.getWidth(), imp.getHeight());
            ArrayList<SparseLabel> labels = new ArrayList<SparseLabel>();
            IJ.log("Creating IP");
            ShortProcessor ip = new ShortProcessor(imp.getWidth(), imp.getHeight());

            IJ.log("Using factory to make labels");
            factory.makeLabels(imp, labels);

            IJ.log("Pushing " + labels.size() + " labels into new image");
            for (SparseLabel sl : labels)
            {
                SparseLabelFactory.addLabelTo(ip, sl);
            }

            IJ.log("Done");

            new ImagePlus("Label copy", ip).show();

        }
    }
}
