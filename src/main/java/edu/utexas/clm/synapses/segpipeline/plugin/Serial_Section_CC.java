package edu.utexas.clm.synapses.segpipeline.plugin;

import edu.utexas.clm.synapses.segpipeline.process.SerialSectionConnectedComponents;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;


public class Serial_Section_CC implements PlugIn

{
    public void run(String s) {
        final SerialSectionConnectedComponents sscc = new SerialSectionConnectedComponents(
                IJ.getImage());
        if (sscc.checkInput() && sscc.process())
        {
            new ImagePlus("Connected Components", sscc.getImageStack()).show();
        }
    }
}
