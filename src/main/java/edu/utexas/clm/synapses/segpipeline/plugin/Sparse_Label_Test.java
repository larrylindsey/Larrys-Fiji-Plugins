package edu.utexas.clm.synapses.segpipeline.plugin;

import edu.utexas.clm.synapses.segpipeline.data.label.*;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.AbstractLabelMorph;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.LabelClose;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.LabelDilate;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.LabelOpen;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ShortProcessor;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.io.ImgOpener;
import net.imglib2.type.numeric.integer.IntType;

import java.util.ArrayList;

/**
 *
 */
public class Sparse_Label_Test implements PlugIn
{

    long st = -1;

    public void run(String s)
    {
        /*ImagePlus imp = IJ.getImage();
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

        }*/

        try
        {
            ImgOpener opener = new ImgOpener();
            Img<IntType> img0 = opener.openImg("/home/larry/image0.tif",
                    new ArrayImgFactory<IntType>(), new IntType());
            Img<IntType> img1 = opener.openImg("/home/larry/image1.tif",
                    new ArrayImgFactory<IntType>(), new IntType());
            SparseLabelFactory factory = new SparseLabelFactory((int)img0.dimension(0),
                    (int)img0.dimension(1));
            LabelDilate ld8 = new LabelDilate(AbstractLabelMorph.diskStrel(8));
            LabelDilate ld7 = new LabelDilate(AbstractLabelMorph.diskStrel(7));

            LabelOpen lo = new LabelOpen(AbstractLabelMorph.diskStrel(8),
                    AbstractLabelMorph.diskStrel(8));
            LabelClose lc = new LabelClose(AbstractLabelMorph.diskStrel(8),
                    AbstractLabelMorph.diskStrel(8));

            tic();
            SparseLabel sl0 = getLabel(factory, img0, 1425);
            toc("Created sl0"); tic();
            SparseLabel sl1 = getLabel(factory, img1, 6571);
            toc("Created sl1"); tic();
//            SparseLabel sl0bd = ld8.process(ld7.process(sl0));
//            toc("Dilated sl0"); tic();
//            SparseLabel sl1bd = ld8.process(ld7.process(sl1));
//            toc("Dilated sl1"); tic();
//            SparseLabel sl0U1 = sl0.union(sl1);
//            toc("Calculated union"); tic();
//            SparseLabel sl0I1 = sl0.intersection(sl1);
//            toc("Calculated intersection"); tic();
            SparseLabel sl0o = lo.process(sl0);
            toc("Opened sl0"); tic();
            SparseLabel sl1o = lo.process(sl1);
            toc("Opened sl1"); tic();
            SparseLabel sl0c = lc.process(sl0);
            toc("Closed sl0"); tic();
            SparseLabel sl1c = lc.process(sl1);
            toc("Closed sl1");



            showLabel(sl0, "sl0");
            showLabel(sl1, "sl1");
//            showLabel(sl0bd, "sl0-dilated");
//            showLabel(sl1bd, "sl1-dilated");
//            showLabel(sl0U1, "sl0 union sl1");
//            showLabel(sl0I1, "sl0 intersect sl1");
            showLabel(sl0o, "sl0-opened");
            showLabel(sl1o, "sl1-opened");
            showLabel(sl0c, "sl0-closed");
            showLabel(sl1c, "sl1-closed");

        }
        catch (Exception e)
        {
            IJ.error("Error: " + e);
            e.printStackTrace();
        }

    }

    private void tic()
    {
        st = System.currentTimeMillis();
    }

    private void toc(String message)
    {
        long pt = System.currentTimeMillis() - st;
        IJ.log(message + ": took " + pt + "ms");
    }

    public SparseLabel getLabel(SparseLabelFactory factory, Img<IntType> img, int l)
    {
        ArrayList<SparseLabel> labels = new ArrayList<SparseLabel>();
        factory.makeLabels(img, 1, labels);
        for (SparseLabel sl : labels)
        {
            if (sl.getValue() == l)
            {
                return sl;
            }
        }
        return new SparseLabel(l, factory.getWidth(), factory.getHeight());
    }

    public void showLabel(SparseLabel sl, String name)
    {
        ShortProcessor sp = new ShortProcessor(sl.getWidth(), sl.getHeight());
        SparseLabelFactory.addLabelTo(sp, sl);
        new ImagePlus(name, sp).show();
    }
}
