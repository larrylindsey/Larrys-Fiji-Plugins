package edu.utexas.clm.synapses.segpipeline.data.label.graph;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

/**
 *
 */
public class CentroidOrientationEccentricityFeature extends SparseLabelNodeFeature
{
    @Override
    public int numDimensions()
    {
        return 4;
    }

    @Override
    public void extractFeature(SparseLabel sl)
    {
        final float[] vector = sl.getFeature();
        final int[] x, y;
        final int w = sl.getWidth();
        final int n = sl.area();
        // Respectively, sum of x, sum of y, sum of x^2, sum of x*y
        int sx1 = 0, sy1 = 0, sx2 = 0, sx1y1 = 0;
        // Moments
        float m11 = 0, m20 = 0, m02 = 0;
        float cx, cy;


        int i = 0;


        x = new int[n];
        y = new int[n];

        for (final int idx : sl.getIdx())
        {
            x[i] = idx % w;
            y[i] = idx / w;

            sx1 += x[i];
            sy1 += y[i];
            sx2 += x[i] * x[i];
            sx1y1 += x[i] * y[i];

            i += 1;
        }

        //Centroid
        cx = ((float)sx1)/((float)n);
        cy = ((float)sy1)/((float)n);
        vector[offset] = cx;
        vector[offset + 1] = cy;

        //Orientation
        vector[offset + 2] = (float)Math.atan2(sx2 * n - (sx1 * sx1), n * sx1y1 - sx1 * sy1);
        /*
        Solving for the best fit line y = mx + b over the points x and y in the SparseLabel, I get
          m = (n * sum(x * y) - sum(x) * sum(y)) / (sum(x^2) * n - (sum(x))^2),
          b = (sum(x^2)*sum(y) - sum(x * y) * sum(x)) / (sum(x^2) * n - (sum(x)^2))
          
         where n is the total number of points. Since m represents the slope, we can find the slope
         angle by taking atan2(rise, run), where rise is the numerator and run is the denominator
         in the formula for m. This nicely avoids the divide-by-zero issue that occurs when
         sum(x^2) * n = sum(x)^2
        */

        for (i = 0; i < n; ++i)
        {
            float monX = ((float)x[i] - cx);
            float monY = ((float)y[i] - cy);
            m11 += monX * monY;
            m20 += monX * monX;
            m02 += monY * monY;
        }

        // Eccentricity. Cribbed from equation 55 on this page:
        // http://homepages.inf.ed.ac.uk/rbf/CVonline/LOCAL_COPIES/MARSHALL/node36.html
        vector[offset + 3] =
                (float)((m20 + m02 + Math.sqrt((m20 - m02)*(m20 - m02) + 4 * m11 * m11)) /
                        (m20 + m02 - Math.sqrt((m20 - m02)*(m20 - m02) + 4 * m11 * m11)));

    }


}
