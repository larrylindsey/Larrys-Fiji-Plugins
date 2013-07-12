package edu.utexas.clm.synapses.segpipeline.plugin;

import edu.utexas.clm.synapses.segpipeline.process.ProbImageToConnectedComponents;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;


public class Serial_Section_CC implements PlugIn

{
    public void run(String s) {
        Img<? extends RealType> img;
        ImagePlus imp = IJ.getImage();

        switch(imp.getType())
        {
            case ImagePlus.GRAY8:
                img = ImagePlusAdapter.wrapByte(imp);
                break;
            case ImagePlus.GRAY16:
                img = ImagePlusAdapter.wrapShort(imp);
                break;
            case ImagePlus.GRAY32:
                img = ImagePlusAdapter.wrapFloat(imp);
                break;
            default:
                IJ.error("Must be a grayscale image");
                return;
        }

        @SuppressWarnings("unchecked")
        final ProbImageToConnectedComponents sscc = new ProbImageToConnectedComponents(img,
                ProbImageToConnectedComponents.erodeDisk(1, img.firstElement()));
        @SuppressWarnings("unchecked")
        final ProbImageToConnectedComponents sscc2 = new ProbImageToConnectedComponents(img);

        if (sscc.checkInput() && sscc.process())
        {
            ImageJFunctions.show(sscc.getLabel());
        }

        if (sscc2.checkInput() && sscc2.process())
        {
            ImageJFunctions.show(sscc2.getLabel());
        }

        ProbImageToConnectedComponents.addToLabel(imp.getProcessor(), 100);

        IJ.log("done");
        imp.show();

    }
}
