package edu.utexas.clm.synapses.segpipeline.plugin;

import edu.utexas.archipelago.ArchipelagoUtils;
import edu.utexas.clm.archipelago.Cluster;
import edu.utexas.clm.archipelago.data.FileChunk;
import edu.utexas.clm.synapses.segpipeline.process.ProbImageToConnectedComponents;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;


public class Serial_Section_CC implements PlugIn

{

    public void run(String s) {

        final ImagePlus imp = IJ.getImage();
        final ArrayList<File> imageFiles = new ArrayList<File>();
        final ArrayList<FileChunk> files;
        final ExecutorService es;
        final GenericDialog gd = new GenericDialog("Serial Section Connected Components");
        final boolean gt;
        final float th;
        final int strelSz;

        gd.addRadioButtonGroup("", new String[]{"Greater Than", "Less Than"}, 2, 1, "Greater Than");
        gd.addNumericField("Threshold [0, 1]", .5, 2);
        gd.addNumericField("Open by (px)", 2, 0);
        gd.setMinimumSize(new Dimension(240, 360));
        gd.setSize(new Dimension(240, 360));
        gd.show();

        if (gd.wasCanceled())
        {
            return;
        }

        gt = gd.getNextRadioButton().equals("Greater Than");
        th = (float)gd.getNextNumber();
        strelSz = (int)gd.getNextNumber();


        if (!Cluster.activeCluster())
        {
            Cluster.getClusterWithUI();
        }

        Cluster.getCluster().waitUntilReady();
        es = Cluster.getCluster().getService(1);

        try
        {
            ArchipelagoUtils.getFileList(imageFiles, imp);
            files = ProbImageToConnectedComponents.batchPITCC(imageFiles, es, th, gt, strelSz);
            new ImagePlus(imp.getTitle() + " Connected Components",
                    ArchipelagoUtils.makeVirtualStack(
                            files, imp.getWidth(), imp.getHeight())).show();
        }
        catch (IOException ioe)
        {
            throw new RuntimeException(ioe);
        }
        catch (ExecutionException ee)
        {
            throw new RuntimeException(ee);
        }
        catch (InterruptedException ie)
        {
            throw new RuntimeException(ie);
        }
    }


}
