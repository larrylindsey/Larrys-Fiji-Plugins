package edu.utexas.clm.synapses.segpipeline.process;

import net.imglib2.img.Img;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.DilateGray;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.ErodeGray;
import net.imglib2.type.numeric.RealType;

import java.util.ArrayList;

/**
 *
 */
public class OpenGray < T extends RealType< T >> implements UnaryOperation< Img<T>, Img<T>>
{
    private final DilateGray<T,Img<T>> dilate;
    private final ErodeGray<T,Img<T>> erode;

    public OpenGray(final long[][] strel)
    {
        dilate = new DilateGray<T, Img<T>>(strel);
        erode = new ErodeGray<T, Img<T>>(strel);
    }

    private OpenGray(DilateGray<T, Img<T>> dilate, ErodeGray<T, Img<T>> erode)
    {
        this.dilate = dilate;
        this.erode = erode;
    }

    public Img<T> compute(Img<T> input, Img<T> output)
    {
        return dilate.compute(erode.compute(input, output), output.copy());
    }

    public UnaryOperation<Img<T>, Img<T>> copy()
    {
        return new OpenGray<T>(dilate.copy(), erode.copy());
    }

    public static long[][] diskStrel(final int r)
    {
        long[][] strel;
        final ArrayList<long[]> v = new ArrayList<long[]>();
        final int r2 = r*r;

        for (int i = 0; i <= r; ++i)
        {
            for (int j = 0; j <= r; ++j)
            {
                if (i*i + j*j <= r2)
                {
                    v.add(new long[]{i,j});
                    v.add(new long[]{-i,j});
                    v.add(new long[]{i,-j});
                    v.add(new long[]{-i,-j});
                }
            }
        }

        strel = new long[v.size()][];

        for (int i = 0; i < v.size(); ++i)
        {
            strel[i] = v.get(i);
        }

        return strel;
    }

}
