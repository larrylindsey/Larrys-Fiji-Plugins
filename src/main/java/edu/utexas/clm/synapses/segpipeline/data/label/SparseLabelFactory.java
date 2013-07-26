package edu.utexas.clm.synapses.segpipeline.data.label;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.IntegerType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;


/**
 *
 */
public class SparseLabelFactory
{

    private static final int BUFFER_SIZE = 64;

    private class LabelBuilder
    {
        private boolean needSort = false;
        private ArrayList<Integer> buffer;

        public LabelBuilder()
        {
            buffer = new ArrayList<Integer>();
        }

        public void append(final int value)
        {
            if (!buffer.isEmpty())
            {
                needSort |= value < buffer.get(buffer.size() - 1);
            }
            buffer.add(value);
        }

        public SparseLabel makeSparseLabel(final int l, final int index)
        {
            int[] slIdx = new int[buffer.size()];
            SparseLabel sl;

            if (needSort)
            {
                Collections.sort(buffer);
            }



            for (int i = 0; i < buffer.size(); ++i)
            {
                slIdx[i] = buffer.get(i);
            }



            sl = new SparseLabel(l, width, height, slIdx);
            sl.setIndex(index);
            return sl;
        }
    }

    private final int width, height;

    public SparseLabelFactory(final int width, final int height)
    {
        this.width = width;
        this.height = height;
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }

    public boolean makeLabels(final ImagePlus imp, final int index,
                              final Collection<SparseLabel> labelsOut)
    {
        if (imp.getWidth() != width || imp.getHeight() != height)
        {
            return false;
        }
        else
        {
            final HashMap<Integer, LabelBuilder> builderMap = new HashMap<Integer, LabelBuilder>();
            LabelBuilder builder = null;
            final ArrayList<Integer> keys;
            int linearLoc, tVal, currLabel = 0;
            final ImageProcessor ip = imp.getProcessor();

            for (int y = 0; y < height; ++y)
            {
                System.out.println("y: " + y);

                for (int x = 0; x < width; ++x)
                {
                    linearLoc = x + y * width;
                    tVal = ip.getPixel(x, y);

                    if (tVal > 0)
                    {

                        if (currLabel != tVal || builder == null)
                        {
                            builder = getOrCreate(builderMap, tVal);
                            currLabel = tVal;
                        }

                        builder.append(linearLoc);
                    }
                }

            }

            keys = new ArrayList<Integer>(builderMap.keySet());
            Collections.sort(keys);

            for (Integer key : keys)
            {
                labelsOut.add(builderMap.get(key).makeSparseLabel(key, index));
            }

            return true;
        }
    }

    public <T extends IntegerType<T>> boolean makeLabels(final Img<T> img,
                                                         final int index,
                                                         final Collection<SparseLabel> labelsOut)
    {
        if (img.dimension(0) != width || img.dimension(1) != height)
        {
            return false;
        }
        else
        {
            final HashMap<Integer, LabelBuilder> builderMap = new HashMap<Integer, LabelBuilder>();
            final Cursor<T> cursor = img.cursor();
            final int[] location = new int[2];
            int linearLoc, tVal, currLabel = 0;
            LabelBuilder builder = null;
            final ArrayList<Integer> keys;

            while (cursor.hasNext())
            {
                cursor.fwd();
                cursor.localize(location);
                linearLoc = location[0] + location[1] * width;
                tVal = cursor.get().getInteger();

                if (currLabel != tVal || builder == null)
                {
                    builder = getOrCreate(builderMap, tVal);
                    currLabel = tVal;
                }

                builder.append(linearLoc);
            }

            keys = new ArrayList<Integer>(builderMap.keySet());
            Collections.sort(keys);

            for (Integer key : keys)
            {
                labelsOut.add(builderMap.get(key).makeSparseLabel(key, index));
            }

            return true;
        }
    }


    public SparseLabel makeLabel(final int label, final Iterable<Integer> vals, final int index)
    {
        LabelBuilder lb = new LabelBuilder();
        for (int i : vals)
        {
            lb.append(i);
        }

        return lb.makeSparseLabel(label, index);
    }



    private LabelBuilder getOrCreate(final HashMap<Integer, LabelBuilder> map, final int val)
    {
        LabelBuilder lb = map.get(val);
        if (lb == null)
        {
            lb = new LabelBuilder();
            map.put(val, lb);
        }
        return lb;
    }

    public static void addLabelTo(ImageProcessor ip, SparseLabel sl)
    {
        final int val = sl.getValue();
        for (int i : sl.getIdx())
        {
            int x = i % sl.getWidth();
            int y = i / sl.getWidth();
            ip.set(x, y, val);
        }
    }

    public static void addLabelTo(ImagePlus imp, SparseLabel sl)
    {
        addLabelTo(imp.getProcessor(), sl);
    }

}
