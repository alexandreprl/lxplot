package com.lxprl.plot.server;

import com.lxprl.plot.commons.ChartType;
import com.lxprl.plot.commons.XJFrame;
import com.lxprl.plot.interfaces.ILxPlotChart;
import com.lxprl.plot.interfaces.ILxPlotServer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Real chart displayed by a server.
 *
 * @author Alexandre Perles
 */
public class LxPlotChart implements ILxPlotChart {
    public static int cols = 1;
    protected static int untitledCount = 1;
    private static JMenuBar menuBar;
    private static JMenu layoutMenu;
    private static XJFrame frame;
    private static int chartCount = 0;
    private static String frameName;
    private static JDesktopPane desktopPane;
    private static ReentrantLock frameLock = new ReentrantLock();
    private final Map<String, XYSeries> series = new TreeMap<String, XYSeries>();
    private final String name;
    private final ILxPlotServer server;
    // private JPanel chartContainer;
    private XYSeriesCollection dataset;
    private int seriesCount = 0;
    private JFreeChart chart;
    private TitledBorder border;
    private ChartType chartType = ChartType.PLOT;
    private ChartPanel chartPanel;
    private String firstSerie;
    private JFrame chartFrame;
    private JInternalFrame internalChartFrame;
    public LxPlotChart(final ILxPlotServer _server) {
        this("Untitled " + (LxPlotChart.untitledCount++), _server);
    }
    public LxPlotChart(final String _name, final ChartType _chartType,
                       final ILxPlotServer _server) {
        name = _name;
        chartType = _chartType;
        server = _server;
        LxPlotChart.chartCount++;
        // getChartContainer(true).add(getChartPanel());
        LxPlotChart.getJFrame();
        LxPlotChart.getDesktopPane().add(getChartInternalFrame());
        // getChartContainer().revalidate();
        // getChartContainer().repaint();
    }
    public LxPlotChart(final String _name, final ILxPlotServer _server) {
        this(_name, ChartType.LINE, _server);
    }

    private static JDesktopPane getDesktopPane() {
        if (LxPlotChart.desktopPane == null) {
            LxPlotChart.desktopPane = new JDesktopPane() {

                @Override
                public Component add(Component comp) {
                    Component t = super.add(comp);
                    refreshLayout();
                    return t;
                }

            };

            LxPlotChart.desktopPane
                    .setDesktopManager(new CustomDesktopManager());
            LxPlotChart.desktopPane.setPreferredSize(new Dimension(900, 600));
        }
        return LxPlotChart.desktopPane;
    }

    private static XJFrame getJFrame() {
        frameLock.lock();
        if (LxPlotChart.frame == null) {
            LxPlotChart.frame = new XJFrame("LxPlot");
            LxPlotChart.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            LxPlotChart.frame.getContentPane().add(
                    (LxPlotChart.getDesktopPane()), BorderLayout.CENTER);
            LxPlotChart.frame.getContentPane().add(getMenuBar(),
                    BorderLayout.NORTH);
            LxPlotChart.frame.pack();
            LxPlotChart.frame.setVisible(true);
        }
        frameLock.unlock();
        return LxPlotChart.frame;
    }

    private static JMenuBar getMenuBar() {
        if (menuBar == null) {
            menuBar = new JMenuBar();
            menuBar.add(getLayoutMenu());
        }
        return menuBar;
    }

    private static JMenu getLayoutMenu() {
        if (layoutMenu == null) {
            layoutMenu = new JMenu("Layout");
            JMenuItem apply = new JMenuItem("Apply");
            apply.addActionListener(l -> refreshLayout());
            layoutMenu.add(apply);
            JMenuItem config = new JMenuItem("Configure ...");
            config.addActionListener(l -> new ConfigWindow());
            layoutMenu.add(config);
        }
        return layoutMenu;
    }

    // private JFrame getFrame() {
    // if (chartFrame == null) {
    // chartFrame = new JFrame(frameName);
    // chartFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    // JScrollPane jScrollPane = new JScrollPane(getChartContainer());
    // jScrollPane.setPreferredSize(new Dimension(320, 240));
    // chartFrame.getContentPane().add(jScrollPane, BorderLayout.CENTER);
    // chartFrame.pack();
    // chartFrame.setVisible(true);
    // }
    // return chartFrame;
    // }

    // private JPanel getChartContainer() {
    // return getChartContainer(false);
    // }

    public static void refreshLayout() {
        int x = 0;
        int width = 10, height = 10;
        JInternalFrame[] allFrames = getDesktopPane().getAllFrames();


        List<JInternalFrame> visibleFrames = new ArrayList<>();
        for (JInternalFrame jInternalFrame : allFrames) {
            if (!jInternalFrame.isIcon()) {
                visibleFrames.add(jInternalFrame);
            }
        }

        int w = getDesktopPane().getWidth();
        int h = getDesktopPane().getHeight() - 30;


        if (visibleFrames.isEmpty())
            return;
        if (visibleFrames.size() == 1) {
            width = w;
            height = h;
        } else if (visibleFrames.size() <= cols) {
            width = w / visibleFrames.size();
            height = h;
        } else {
            width = w / cols;
            int rowCount = (int) (visibleFrames.size() / cols)
                    + (visibleFrames.size() % cols == 0 ? 0 : 1);
            height = h / rowCount;
        }
        if (width < 10)
            width = 10;
        if (height < 80)
            height = 80;

        for (JInternalFrame frame : visibleFrames) {
            frame.setLocation(width * (x % cols), height
                    * ((int) (x / cols)));
            frame.setSize(width, height);
            x++;
        }
    }

    public static void setFrameName(final String _name) {
        LxPlotChart.frameName = _name;
    }

    @Override
    public void add(final double _x, final double _y) {
        if (firstSerie == null) {
            firstSerie = "Default";
        }
        add(firstSerie, _x, _y);
    }

    @Override
    public void add(final String _serieName, final double _x, final double _y) {
        if (chartType == ChartType.PLOT) {
            getSeries(_serieName).add(_x, _y);
        } else {
            getSeries(_serieName).addOrUpdate(_x, _y);
        }

    }

    @Override
    public void close() {
        getChartInternalFrame().dispose();
    }

    // public void close() {
    // getChartContainer().remove(getChartPanel());
    // getChartContainer().revalidate();
    // getChartContainer().repaint();
    // }

    // private JPanel getChartContainer(boolean _refresh) {
    // if (chartContainer == null) {
    // chartContainer = new JPanel();
    // }
    // if (_refresh) {
    // switch (chartCount) {
    // case 1:
    // case 2:
    // chartContainer.setLayout(new GridLayout(0, chartCount));
    // break;
    // default:
    // chartContainer.setLayout(new GridLayout(0, 3));
    // break;
    // }
    // }
    // return chartContainer;
    // }
    private JInternalFrame getChartInternalFrame() {
        if (internalChartFrame == null) {
            internalChartFrame = new JInternalFrame(name + " ("
                    + (LxPlotChart.chartCount) + ")", true, true, true, true);
            // final int wx = 900 / LxPlotChart.cols;
            // final int wy = 600 / LxPlotChart.rows;
            // internalChartFrame
            // .setBounds(
            // wx
            // * ((LxPlotChart.chartCount - 1) % LxPlotChart.cols),
            // wy
            // * ((LxPlotChart.chartCount - 1) / LxPlotChart.cols),
            // wx, wy);

            internalChartFrame.add(getChartPanel());
            internalChartFrame.setVisible(true);
        }
        return internalChartFrame;
    }

    private ChartPanel getChartPanel() {
        // we put the chart into a panel
        if (chartPanel == null) {
            chartPanel = new ChartPanel(getJFreeChart());
            border = BorderFactory.createTitledBorder(name);
            chartPanel.setBorder(border);
            chartPanel.setMinimumSize(new Dimension(10, 10));
            // default size
            // chartPanel.setPreferredSize(new Dimension(300, 300));
        }
        return chartPanel;
    }

    private XYSeriesCollection getDataset() {
        if (dataset == null) {
            dataset = new XYSeriesCollection();
        }
        return dataset;
    }

    public JFreeChart getJFreeChart() {
        if (chart == null) {
            switch (chartType) {
                case LINE:
                    chart = ChartFactory.createXYLineChart("", // chart
                            // title
                            "X", // x axis label
                            "Y", // y axis label
                            getDataset(), // data
                            PlotOrientation.VERTICAL, true, // include legend
                            true, // tooltips
                            false // urls
                    );
                    break;
                case PLOT:
                    chart = ChartFactory.createScatterPlot("", // chart
                            // title
                            "X", // x axis label
                            "Y", // y axis label
                            getDataset(), // data
                            PlotOrientation.VERTICAL, true, // include legend
                            true, // tooltips
                            false // urls
                    );
                    break;

            }
            final XYPlot plot = (XYPlot) chart.getPlot();

            NumberAxis range = (NumberAxis) plot.getRangeAxis();
            range.setAutoRange(true);
            range.setAutoRangeIncludesZero(false);
            plot.setBackgroundPaint(Color.white);
            plot.setRangeGridlinePaint(Color.black);
        }
        return chart;
    }

    private XYSeries getSeries(final String _serieName) {
        if (firstSerie == null) {
            firstSerie = _serieName;
        }
        if (!series.containsKey(_serieName)) {
            final XYSeries xySeries = new XYSeries(_serieName, true,
                    (chartType == ChartType.PLOT));

            series.put(_serieName, xySeries);
            getDataset().addSeries(xySeries);
            // ((XYPlot)(getChart().getPlot())).getRenderer().setSeriesStroke(seriesCount,
            // new BasicStroke(3f));
            seriesCount++;
        }
        return series.get(_serieName);
    }
}
