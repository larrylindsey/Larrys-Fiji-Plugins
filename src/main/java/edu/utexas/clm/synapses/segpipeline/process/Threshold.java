package edu.utexas.clm.synapses.segpipeline.process;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import java.io.Serializable;

/**
 *
 */
public class Threshold<T extends RealType<T>>
        implements UnaryOperation<Img<T>, Img<BitType>>, Serializable
{
    private final T t;
    private final boolean gt;

    public Threshold(T t, boolean gt)
    {
        this.t = t;
        this.gt = gt;
    }


    public Img<BitType> compute(Img<T> imgIn, Img<BitType> imgOut)
    {
        final Cursor<BitType> target = imgOut.localizingCursor();
        final RandomAccess<T> source = imgIn.randomAccess();
        boolean test;

        while ( target.hasNext())
        {
            target.fwd();
            source.setPosition(target);
            test = source.get().compareTo(t) > 0;
            target.get().set(test == gt);
        }
        return imgOut;
    }

    public UnaryOperation<Img<T>, Img<BitType>> copy() {
        return null;
    }

    public static <T extends RealType<T>> T fractionOfMax(Img<T> img, double fraction)
    {
        double maxVal = 0;
        final Cursor<T> cursor = img.cursor();
        final T t;

        while (cursor.hasNext())
        {
            final double val;
            cursor.fwd();

            val = cursor.get().getRealDouble();

            if (val > maxVal)
            {
                maxVal = val;
            }
        }

        t = img.firstElement().copy();
        t.setReal(maxVal * fraction);

        return t;
    }
}
