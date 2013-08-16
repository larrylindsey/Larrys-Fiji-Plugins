package edu.utexas.clm.synapses.segpipeline.data.graph.feature;

import edu.utexas.clm.synapses.segpipeline.data.graph.SVEGFactory;
import edu.utexas.clm.synapses.segpipeline.data.label.SerialSparseLabels;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.ChainOperation;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.DilatedBorderOperation;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.LabelDilate;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.LabelOperation;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.Strels;
import ij.IJ;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

/**
 *
 */
public class ImageHistogramSimilarityFeature extends SparseLabelEdgeFeature
{

    private final int nHist;
    private final float imageMax;
    private final LabelOperation borderOp;

    public ImageHistogramSimilarityFeature()
    {
        final int[][] strel8 = Strels.diskStrel(8);
        nHist = 255;
        imageMax = 255;
        borderOp = new DilatedBorderOperation(strel8);
    }

    @Override
    public int numDimensions()
    {
        return 1;
    }

    @Override
    public void extractFeature(final SVEGFactory factory, final SparseLabel sl0,
                               final SparseLabel sl1, final int offset)
    {
        // Assume sl0 and sl1 overlap across one index, or are neighboring labels.
        // This is enforced by the accept function

        final int[] h0 = new int[nHist], h1 = new int[nHist];

        if (sl0.getIndex() == sl1.getIndex())
        {
            inPlaneSimilarity(factory.getLabels(), sl0, sl1, h0, h1);
        }
        else
        {
            if (crossPlainSimilarity(factory.getLabels(), sl0, sl1, h0, h1))
            {
                factory.getVector(sl0, sl1)[offset] = histogramMetric(h0, h1);
            }
        }
    }

    public float histogramMetric(final int[] h0, final int[] h1)
    {
        float minSum = 0, maxSum = 0;

        float h0s = 0, h1s = 0;

        for (int h : h0)
        {
            h0s += h;
        }

        for (int h : h1)
        {
            h1s += h;
        }

        for (int i = 0; i < h0.length; ++i)
        {
            minSum += Math.min(((float)h0[i]) / h0s, ((float)h1[i]/h1s));
            maxSum += Math.max(((float)h0[i]) / h0s, ((float)h1[i]/h1s));
        }
        return minSum / maxSum;
    }


    public boolean inPlaneSimilarity(final SerialSparseLabels labels,
                                   final SparseLabel sl0, final SparseLabel sl1,
                                   final int[] h0, final int[] h1)
    {
        assert sl0.getIndex() == sl1.getIndex();
        final Img<? extends RealType> img = labels.getImage(sl0);
        if (img == null)
        {
            return false;
        }
        else
        {
            final SparseLabel nbd = borderOp.process(sl0).intersection(borderOp.process(sl1));
            histogram(sl0.intersection(nbd), img, h0);
            histogram(sl1.intersection(nbd), img, h1);
            return true;
        }
    }

    public boolean crossPlainSimilarity(final SerialSparseLabels labels,
                                      final SparseLabel sl0, final SparseLabel sl1,
                                      final int[] h0, final int[] h1)
    {
        final Img<? extends RealType> img0, img1;
        if ((img0 = labels.getImage(sl0)) != null && (img1 = labels.getImage(sl1)) != null)
        {

            histogram(sl0, img0, h0);
            histogram(sl1, img1, h1);
            return true;
        }
        else
        {
            return false;
        }
    }

    public void histogram(final SparseLabel sl, final Img<? extends RealType> img, final int[] h)
    {
        final RandomAccess<? extends RealType> ra = img.randomAccess();
        final long[] pos = new long[2];
        final float binWidth = imageMax / (float)nHist;
        int bin;

        for (int idx : sl.getIdx())
        {
            pos[0] = idx % sl.getWidth();
            pos[1] = idx / sl.getWidth();
            ra.setPosition(pos);
            try
            {
                bin = (int)(ra.get().getRealFloat() / binWidth);
            }
            catch (ArrayIndexOutOfBoundsException aioobe)
            {
                IJ.log("Caught OOBE at " + pos[0] + ", " + pos[1] + ". Linear idx: " + idx);
                IJ.log("Label was size " + sl.getWidth() + " " + sl.getHeight());
                throw aioobe;
            }

            if (bin >= h.length)
            {
                bin = h.length - 1;
            }

            ++h[bin];
        }
    }

    @Override
    public Iterable<SparseLabel> accept(SVEGFactory factory, SparseLabel sl)
    {
        return acceptAllNeighbors(factory, sl);
    }

    @Override
    public String name()
    {
        return "Histogram Similiarity";
    }
}
