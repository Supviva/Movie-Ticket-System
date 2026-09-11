package com.movieticket.ui.component;

import com.movieticket.model.Movie;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.BoxLayout;
import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * 首页主视觉横幅：大图打底 + 左侧渐变遮罩压暗 + 影片信息与购票入口。
 *
 * <p>取代原来「圆形图标 + 两行文案 + 分页点」的纯文字轮播——首屏最贵的位置应当
 * 承载一部真实影片，让用户一眼看到海报、评分和购票入口。</p>
 *
 * <p>支持自动轮播（默认 5.5 秒，鼠标悬停时暂停）与左右箭头、缩略图手动切换；
 * 切换主推影片可调用 {@link #show(int)} 或 {@link #advance()}。</p>
 */
public final class HeroBannerPanel extends JPanel {

    private static final int HEIGHT = 340;
    private static final int THUMB_W = 52;
    private static final int THUMB_H = 74;
    /** 海报卡片高度：内容区高 = HEIGHT - 上下 border 60。 */
    private static final int CARD_H = 280;
    /** 自动轮播间隔。5.5 秒够读完标题、评分与简介首行，也不至于让人等太久。 */
    private static final int AUTO_DELAY_MS = 5500;
    /** 左右箭头热区宽度（px），点击该区域即切换上一张/下一张。 */
    private static final int ARROW_ZONE = 48;

    private final List<Movie> movies;
    private final Consumer<Movie> onBuy;
    private final Consumer<Movie> onDetail;
    private final Runnable onChanged;

    private int index;
    /** 氛围背景：海报降采样小图，绘制时双线性放大即成模糊层。 */
    private BufferedImage ambient;
    private javax.swing.Timer autoTimer;
    private boolean hovering;
    /** 定时器是否因组件被摘除（removeNotify）而停表，重新挂载时据此恢复。 */
    private boolean detached;
    /** 鼠标所在箭头热区：-1 左、1 右、0 都不在。 */
    private int hoverZone;

    /**
     * @param movies    主推影片列表，至少一部
     * @param onBuy     点击「选座购票」
     * @param onDetail  点击「查看详情」
     * @param onChanged 切换主推影片后的回调，供外层刷新缩略图选中态
     */
    public HeroBannerPanel(List<Movie> movies, Consumer<Movie> onBuy,
                           Consumer<Movie> onDetail, Runnable onChanged) {
        this.movies = List.copyOf(movies);
        this.onBuy = onBuy;
        this.onDetail = onDetail;
        this.onChanged = onChanged;

        setOpaque(false);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, HEIGHT));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        if (!this.movies.isEmpty()) {
            loadBackdrop();
        }
        add(buildContent(), BorderLayout.CENTER);
        installHoverPause();
        installArrowClicks();
        if (this.movies.size() > 1) {
            autoTimer = new javax.swing.Timer(AUTO_DELAY_MS, event -> advance());
            autoTimer.start();
        }
    }

    /** 当前主推影片。 */
    public Movie current() {
        return movies.isEmpty() ? null : movies.get(index);
    }

    /** 当前主推影片的下标。 */
    public int index() {
        return index;
    }

    /** 自动轮播是否正在运行。 */
    public boolean isAutoPlaying() {
        return autoTimer != null && autoTimer.isRunning();
    }

    /** 暂停自动轮播（手动操作时使用；重复调用无副作用）。 */
    public void pause() {
        if (autoTimer != null) {
            autoTimer.stop();
        }
    }

    /** 恢复自动轮播（单部影片或已挂载前调用无副作用）。 */
    public void resume() {
        if (autoTimer != null && movies.size() > 1) {
            autoTimer.start();
        }
    }

    /** 切换到第 {@code i} 部主推影片。已在该部上时保持幂等，不做重绘。 */
    public void show(int i) {
        if (movies.isEmpty() || i < 0 || i >= movies.size() || i == index) {
            return;
        }
        index = i;
        loadBackdrop();
        removeAll();
        add(buildContent(), BorderLayout.CENTER);
        revalidate();
        repaint();
        if (onChanged != null) {
            onChanged.run();
        }
    }

    /** 推进到下一部主推影片（自动轮播与右箭头共用）。 */
    void advance() {
        show(nextIndex(index, movies.size()));
    }

    /** 循环推进：回到末尾后折返第 0 部；无影片时返回 0。 */
    static int nextIndex(int current, int size) {
        if (size <= 0) {
            return 0;
        }
        return (current + 1) % size;
    }

    /**
     * 自动轮播的生命周期与组件挂载绑定。
     *
     * <p>外层 {@code MovieBrowseView.refresh()} 会直接丢弃旧 hero 实例，
     * {@code null} 不会触发任何回调，所以必须在 {@code removeNotify()} 里停表，
     * 否则旧 Timer 会持有引用继续空转；重新挂载时（切回首页）恢复。</p>
     */
    @Override
    public void addNotify() {
        super.addNotify();
        if (autoTimer != null && detached && !hovering) {
            autoTimer.start();
        }
        detached = false;
    }

    @Override
    public void removeNotify() {
        if (autoTimer != null) {
            autoTimer.stop();
            detached = true;
        }
        super.removeNotify();
    }

    /**
     * 悬停暂停：鼠标进入主视觉即暂停，真正离开才恢复。
     *
     * <p>监听器挂在 hero 自身而不是按钮/缩略图上——子组件会反复触发
     * enter/exited 造成抖动。{@code mouseExited} 里用坐标二次确认指针
     * 确实移出了 hero 边界，避免「移到子组件上」被误判为离开。</p>
     */
    private void installHoverPause() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                setHovering(true);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                java.awt.Point point = javax.swing.SwingUtilities.convertPoint(
                        event.getComponent(), event.getPoint(), HeroBannerPanel.this);
                if (!contains(point)) {
                    setHovering(false);
                }
            }
        });
    }

    private void setHovering(boolean value) {
        if (hovering == value) {
            return;
        }
        hovering = value;
        if (value) {
            pause();
        } else {
            resume();
        }
    }

    /** 左右箭头的点击热区与悬停加亮（绘制见 {@link #drawArrows}）。 */
    private void installArrowClicks() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (movies.size() < 2) {
                    return;
                }
                if (event.getX() <= ARROW_ZONE) {
                    show((index - 1 + movies.size()) % movies.size());
                } else if (event.getX() >= getWidth() - ARROW_ZONE) {
                    show(nextIndex(index, movies.size()));
                }
            }
        });
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                int zone = event.getX() <= ARROW_ZONE ? -1
                        : event.getX() >= getWidth() - ARROW_ZONE ? 1 : 0;
                if (zone != hoverZone) {
                    hoverZone = zone;
                    repaint();
                }
            }
        });
    }

    /** 参考宽高比：氛围横带按此从海报取条带，绘制时再拉伸到实际面板大小。 */
    private static final double AMBIENT_ASPECT = 1240.0 / 340;
    /** 氛围小图的目标宽度——足够小才够「糊」，双线性放大后就是柔和的色洗。 */
    private static final int AMBIENT_W = 160;

    private void loadBackdrop() {
        Movie movie = movies.get(index);
        BufferedImage poster = load(movie.id());
        ambient = null;
        if (poster == null) {
            return;
        }
        // 从海报上取一条与横幅同宽高比的横带（焦点偏上部：海报主体在上方），
        // 降采样成小图；绘制时拉伸铺满即得到柔和的模糊氛围层
        int imgW = poster.getWidth();
        int imgH = poster.getHeight();
        int bandH = Math.min(imgH, (int) Math.round(imgW / AMBIENT_ASPECT));
        int bandY = (int) ((imgH - bandH) * 0.25);
        int ambientH = Math.max(1, (int) Math.round(AMBIENT_W / AMBIENT_ASPECT));
        BufferedImage small = new BufferedImage(
                AMBIENT_W, ambientH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = small.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(poster.getSubimage(0, bandY, imgW, bandH),
                0, 0, AMBIENT_W, ambientH, null);
        g.dispose();
        ambient = small;
    }

    // ---------- 绘制 ----------

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        g2.clip(new RoundRectangle2D.Float(0, 0, width, height, 32, 32));

        if (ambient != null) {
            g2.drawImage(ambient, 0, 0, width, height, null);
        } else {
            g2.setColor(Ui.SURFACE);
            g2.fillRect(0, 0, width, height);
        }
        drawScrim(g2, width, height);
        drawArrows(g2, width, height);
        g2.dispose();
    }

    /** 左深右浅的横向遮罩 + 底部纵向压暗，保证文字在任何海报上都可读。 */
    private void drawScrim(Graphics2D g2, int width, int height) {
        g2.setPaint(new LinearGradientPaint(0, 0, width, 0,
                new float[]{0f, 0.38f, 0.72f, 1f},
                new Color[]{new Color(18, 18, 18, 245), new Color(18, 18, 18, 210),
                        new Color(18, 18, 18, 64), new Color(18, 18, 18, 140)}));
        g2.fillRect(0, 0, width, height);

        g2.setPaint(new LinearGradientPaint(0, height, 0, 0,
                new float[]{0f, 0.55f, 1f},
                new Color[]{new Color(18, 18, 18, 230), new Color(18, 18, 18, 0),
                        new Color(18, 18, 18, 0)}));
        g2.fillRect(0, 0, width, height);
    }

    /** 左右切换箭头：半透明圆形 chevron，悬停热区加亮。只绘制不占布局。 */
    private void drawArrows(Graphics2D g2, int width, int height) {
        if (movies.size() < 2) {
            return;
        }
        int diameter = 38;
        int cy = height / 2 - diameter / 2;
        drawChevron(g2, 14, cy, diameter, hoverZone == -1, true);
        drawChevron(g2, width - 14 - diameter, cy, diameter, hoverZone == 1, false);
    }

    private void drawChevron(Graphics2D g2, int x, int y, int diameter,
                             boolean hot, boolean left) {
        g2.setColor(new Color(18, 18, 18, hot ? 150 : 92));
        g2.fillOval(x, y, diameter, diameter);
        g2.setColor(new Color(255, 255, 255, hot ? 235 : 170));
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int cx = x + diameter / 2;
        int cy = y + diameter / 2;
        int r = 6;
        if (left) {
            g2.drawPolyline(new int[]{cx + r, cx - r, cx + r}, new int[]{cy - r, cy, cy + r}, 3);
        } else {
            g2.drawPolyline(new int[]{cx - r, cx + r, cx - r}, new int[]{cy - r, cy, cy + r}, 3);
        }
    }

    // ---------- 内容 ----------

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(26, 0));
        content.setOpaque(false);
        // 左边距给左右箭头浮层让位，避免评分/标题文字与箭头挤在一起
        content.setBorder(BorderFactory.createEmptyBorder(30, 72, 30, 56));

        content.add(buildInfo(), BorderLayout.CENTER);
        content.add(buildRightZone(), BorderLayout.EAST);
        return content;
    }

    /** 右侧区：完整海报卡片（WEST 纵向撑满）+ 缩略图切换条（CENTER，glue 压到底部）。 */
    private JComponent buildRightZone() {
        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        if (current() != null) {
            right.add(new PosterCard(current()), BorderLayout.WEST);
        }
        right.add(buildThumbs(), BorderLayout.CENTER);
        return right;
    }

    private JPanel buildInfo() {
        Movie movie = current();
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        if (movie == null) {
            info.add(Box.createVerticalGlue());
            return info;
        }

        JLabel tag = pill("● 正在热映");
        tag.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(Box.createVerticalGlue());
        info.add(tag);
        info.add(Box.createVerticalStrut(12));

        JLabel title = new JLabel(movie.title());
        title.setFont(Ui.font(Font.BOLD, 32));
        title.setForeground(Ui.INK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(title);
        info.add(Box.createVerticalStrut(10));

        info.add(metaRow(movie));
        info.add(Box.createVerticalStrut(12));

        JLabel desc = new JLabel("<html><div style='width:400px'>" + escape(movie.synopsis()) + "</div></html>");
        desc.setFont(Ui.font(Font.PLAIN, 13));
        desc.setForeground(Ui.MUTED);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(desc);
        info.add(Box.createVerticalStrut(20));
        info.add(buildActions(movie));
        return info;
    }

    /** 评分 · 类型 · 时长 · 格式 · 导演，以圆点分隔。 */
    private JPanel metaRow(Movie movie) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel score = new JLabel(String.format(java.util.Locale.ROOT, "%.1f", movie.rating()));
        score.setFont(Ui.font(Font.BOLD, 15));
        score.setForeground(new Color(0xFF, 0xB4, 0x00));
        row.add(score);

        for (String text : new String[]{movie.genre(), movie.durationMinutes() + " 分钟",
                movie.format(), movie.director()}) {
            row.add(dot());
            JLabel label = new JLabel(text);
            label.setFont(Ui.font(Font.PLAIN, 13));
            label.setForeground(Ui.INK_SOFT);
            row.add(label);
        }
        row.add(Box.createHorizontalGlue());
        return row;
    }

    private static JLabel dot() {
        JLabel spacer = new JLabel("  ·  ");
        spacer.setFont(Ui.font(Font.PLAIN, 13));
        spacer.setForeground(new Color(0x66, 0x66, 0x66));
        return spacer;
    }

    private JPanel buildActions(Movie movie) {
        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton buy = solidButton("选座购票");
        buy.addActionListener(event -> onBuy.accept(movie));
        actions.add(buy);
        actions.add(Box.createHorizontalStrut(12));

        JButton detail = ghostButton("查看详情");
        detail.addActionListener(event -> onDetail.accept(movie));
        actions.add(detail);
        actions.add(Box.createHorizontalGlue());
        return actions;
    }

    private JButton solidButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? Ui.PRIMARY_HOVER : Ui.PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(graphics);
            }
        };
        style(button, Ui.CANVAS, 14);
        button.setBorder(BorderFactory.createEmptyBorder(11, 26, 11, 26));
        return button;
    }

    private JButton ghostButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, getModel().isRollover() ? 36 : 20));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(255, 255, 255, 46));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(graphics);
            }
        };
        style(button, Ui.INK, 14);
        button.setBorder(BorderFactory.createEmptyBorder(11, 22, 11, 22));
        return button;
    }

    private static void style(JButton button, Color foreground, int size) {
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setForeground(foreground);
        button.setFont(Ui.font(Font.BOLD, size));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** 底部缩略图切换条 + 指示点，纵向排列并靠底对齐。 */
    private JPanel buildThumbs() {
        JPanel holder = new JPanel();
        holder.setOpaque(false);
        holder.setLayout(new BoxLayout(holder, BoxLayout.Y_AXIS));

        JPanel strip = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        strip.setOpaque(false);
        for (int i = 0; i < movies.size(); i++) {
            strip.add(thumb(movies.get(i), i));
        }
        // BoxLayout 会把富余高度平均分给所有可伸展的子组件；
        // 锁死 strip/dots 的最大高度，让 glue 独占富余空间，条才能贴底
        strip.setMaximumSize(new Dimension(Integer.MAX_VALUE, strip.getPreferredSize().height));
        JComponent dots = buildDots();
        dots.setMaximumSize(new Dimension(Integer.MAX_VALUE, dots.getPreferredSize().height));
        holder.add(Box.createVerticalGlue());
        holder.add(strip);
        holder.add(dots);
        return holder;
    }

    /** 指示点：与缩略图条互补，只用颜色表达当前第几部。 */
    private JComponent buildDots() {
        JPanel dots = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 7, 6));
        dots.setOpaque(false);
        dots.setAlignmentX(Component.RIGHT_ALIGNMENT);
        for (int i = 0; i < movies.size(); i++) {
            final int dot = i;
            JPanel point = new JPanel() {
                @Override
                protected void paintComponent(Graphics graphics) {
                    Graphics2D g2 = (Graphics2D) graphics.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(dot == index ? Ui.PRIMARY : new Color(255, 255, 255, 64));
                    g2.fillOval(0, 0, 8, 8);
                    g2.dispose();
                }
            };
            point.setOpaque(false);
            point.setPreferredSize(new Dimension(8, 8));
            point.setMaximumSize(new Dimension(8, 8));
            dots.add(point);
        }
        return dots;
    }
    private JPanel thumb(Movie movie, int i) {
        HeroThumb panel = new HeroThumb(movie, i == index);
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                show(i);
            }
        });
        return panel;
    }

    /** 读取海报原图；缺图返回 null（缩略图与海报卡片共用）。 */
    static BufferedImage load(String id) {
        try (InputStream in = HeroBannerPanel.class.getResourceAsStream("/posters/" + id + ".jpg")) {
            return in == null ? null : ImageIO.read(in);
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * 右侧完整海报卡片：不裁切地展示当前主推影片，点击进入详情。
     *
     * <p>取代原来「海报放大 4 倍糊满横幅」的铺法——竖版海报放进横幅
     * 只能完整地立在边上，背景交给模糊氛围层。</p>
     */
    private final class PosterCard extends JPanel {
        private final Movie movie;
        private final BufferedImage image;

        private PosterCard(Movie movie) {
            this.movie = movie;
            this.image = movie == null ? null : load(movie.id());
            setOpaque(false);
            int width = Math.round(CARD_H * 2f / 3);
            setPreferredSize(new Dimension(width, CARD_H));
            setMaximumSize(new Dimension(width, CARD_H));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (movie != null) {
                setToolTipText(movie.title());
            }
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    if (PosterCard.this.movie != null) {
                        onDetail.accept(PosterCard.this.movie);
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int w = getWidth();
            int h = getHeight();
            RoundRectangle2D.Float clip = new RoundRectangle2D.Float(0, 0, w, h, 18, 18);
            g2.setClip(clip);
            if (image != null) {
                int imgW = image.getWidth();
                int imgH = image.getHeight();
                double scale = Math.max((double) w / imgW, (double) h / imgH);
                int drawW = (int) Math.round(imgW * scale);
                int drawH = (int) Math.round(imgH * scale);
                g2.drawImage(image, (w - drawW) / 2, (h - drawH) / 2, drawW, drawH, null);
            } else {
                g2.setColor(Ui.SURFACE);
                g2.fillRect(0, 0, w, h);
                drawFallbackGlyph(g2, w, h);
            }
            g2.setClip(null);
            g2.setColor(new Color(255, 255, 255, 46));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(clip);
            g2.dispose();
        }

        /** 缺图时的占位：几何底 + 居中标识字（与 PosterPanel 同一套标识）。 */
        private void drawFallbackGlyph(Graphics2D g2, int w, int h) {
            String glyph = com.movieticket.ui.component.PosterPanel
                    .fallbackGlyph(movie == null ? "" : movie.title());
            if (glyph.isEmpty()) {
                return;
            }
            g2.setColor(new Color(255, 255, 255, 200));
            g2.setFont(Ui.font(Font.BOLD, Math.max(28, w / 3)));
            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(glyph, (w - metrics.stringWidth(glyph)) / 2,
                    (h - metrics.getHeight()) / 2 + metrics.getAscent());
        }
    }

    /** 单个缩略图：未选中半透明，选中加主色描边。 */
    private static final class HeroThumb extends JPanel {
        private final Movie movie;
        private final boolean active;
        private final BufferedImage image;

        private HeroThumb(Movie movie, boolean active) {
            this.movie = movie;
            this.active = active;
            this.image = load(movie.id());
            setOpaque(false);
            setPreferredSize(new Dimension(THUMB_W + 4, THUMB_H + 4));
            setMaximumSize(new Dimension(THUMB_W + 4, THUMB_H + 4));
            setToolTipText(movie.title());
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int x = 2;
            int y = 2;
            g2.clip(new RoundRectangle2D.Float(x, y, THUMB_W, THUMB_H, 16, 16));
            if (image != null) {
                double scale = Math.max((double) THUMB_W / image.getWidth(), (double) THUMB_H / image.getHeight());
                int w = (int) Math.round(image.getWidth() * scale);
                int h = (int) Math.round(image.getHeight() * scale);
                g2.drawImage(image, x + (THUMB_W - w) / 2, y + (THUMB_H - h) / 2, w, h, null);
            } else {
                g2.setColor(Ui.SURFACE);
                g2.fillRoundRect(x, y, THUMB_W, THUMB_H, 16, 16);
            }
            if (!active) {
                g2.setColor(new Color(18, 18, 18, 160));
                g2.fillRoundRect(x, y, THUMB_W, THUMB_H, 16, 16);
            }
            g2.setClip(null);
            if (active) {
                g2.setColor(Ui.PRIMARY);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(x, y, THUMB_W - 1, THUMB_H - 1, 16, 16);
            }
            g2.dispose();
        }
    }

    /** 「● 正在热映」标记，主色描边胶囊。 */
    private static JLabel pill(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 115, 0, 41));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);
                g2.setColor(new Color(255, 115, 0, 115));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 40, 40);
                g2.dispose();
                super.paintComponent(graphics);
            }
        };
        label.setOpaque(false);
        label.setForeground(Ui.PRIMARY);
        label.setFont(Ui.font(Font.BOLD, 11));
        label.setBorder(BorderFactory.createEmptyBorder(5, 11, 5, 11));
        return label;
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** 供外层排版参考的高度。 */
    public static int preferredHeight() {
        return HEIGHT;
    }
}
