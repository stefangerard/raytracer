package de.fhkl.imst.i.cgma.raytracer.gui;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.concurrent.TimeUnit;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EtchedBorder;

import de.fhkl.imst.i.cgma.raytracer.Raytracer06;

public class RayTracerStatusBar extends JPanel implements RayTracerGui.RayTracerProgressChangedListener,
        RayTracerGui.RayTracerResolutionChangedListener, MouseListener {

    private static final long serialVersionUID = 1L;
    private JProgressBar progressBar = new JProgressBar();
    private JLabel resolutionLabel = new JLabel();
    private static JLabel clientLabel = new JLabel();

    private boolean showTime = true;

    private long tDelta;
    private int points;
    private int pointsSet;

    private String clientLabelStr;

    protected RayTracerStatusBar() {

        progressBar.setStringPainted(true);
        progressBar.addMouseListener(this);
        updateProgressText();

        setBorder(new EtchedBorder());

        setLayout(new BorderLayout());
        add(progressBar, BorderLayout.EAST);
        add(resolutionLabel, BorderLayout.WEST);
        add(Box.createHorizontalStrut(10));
        add(clientLabel, BorderLayout.CENTER);
    }

    public static void setClientLabel(String label) {
        label = "\t\t\t\t" + label;
        String clientLabelOld = clientLabel.getText();
        clientLabel.setText(label);
        if (!clientLabelOld.equals("")) {
            if (Raytracer06.INSTANCE.client == false) {
                setClientOrServer("Server");
            }
        }
    }

    public static void setClientOrServer(String label) {
        label = "\t\t\t\t" + label + clientLabel.getText();
        clientLabel.setText(label);
    }

    @Override
    public void rayTraceResolutionChanged(int resx, int resy) {
        resolutionLabel.setText(String.format("%dx%d", resx, resy));
    }

    @Override
    public void rayTraceProgressChanged(long tDelta, int points, int pointsSet) {
        progressBar.setMaximum(points);
        progressBar.setValue(pointsSet);
        this.tDelta = tDelta;
        this.points = points;
        this.pointsSet = pointsSet;
        updateProgressText();
    }

    private void updateProgressText() {
        if (showTime) {
            progressBar.setString(millisToString(tDelta));
        } else {
            progressBar.setString(String.format("%d/%d", pointsSet, points));
        }
    }

    private String millisToString(long millis) {
        long sec, min;
        min = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(min);
        sec = TimeUnit.MILLISECONDS.toSeconds(millis);
        millis -= TimeUnit.SECONDS.toMillis(sec);
        return String.format("%02d:%02d:%03d", min, sec, millis);
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
        if (arg0.getComponent() == progressBar) {
            showTime = !showTime;
            updateProgressText();
        }
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
    }

    @Override
    public void mousePressed(MouseEvent arg0) {
    }

    @Override
    public void mouseReleased(MouseEvent arg0) {
    }
}
