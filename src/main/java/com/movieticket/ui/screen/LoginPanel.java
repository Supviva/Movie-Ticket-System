package com.movieticket.ui.screen;

import com.movieticket.demo.DemoException;
import com.movieticket.demo.DemoMovieTicketService;
import com.movieticket.model.Account;
import com.movieticket.ui.component.CuteMovieLogo;
import com.movieticket.ui.component.Ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public final class LoginPanel extends JPanel {

    private static final Color LIGHT_BG = new Color(0xF2, 0xF3, 0xF3);
    private static final Color DARK_TEXT = new Color(0x12, 0x12, 0x12);
    private static final Color INPUT_BORDER = new Color(0xE6, 0xE6, 0xE6);

    private final DemoMovieTicketService service;
    private final Consumer<Account> onAuthenticated;
    private final JPanel formStack = new JPanel();
    private final JLabel messageLabel = new JLabel(" ");
    private final JTextField loginUser = field(22);
    private final JPasswordField loginPassword = password(22);
    private final JTextField registerUser = field(22);
    private final JTextField registerName = field(22);
    private final JTextField registerPhone = field(22);
    private final JPasswordField registerPassword = password(22);
    private final JPasswordField registerConfirm = password(22);
    private final JLabel registerMessage = new JLabel(" ");

    public LoginPanel(DemoMovieTicketService service, Consumer<Account> onAuthenticated) {
        this.service = service;
        this.onAuthenticated = onAuthenticated;
        setLayout(new BorderLayout());
        setBackground(Ui.CANVAS);

        add(createBrand(), BorderLayout.WEST);
        add(createFormArea(), BorderLayout.CENTER);
    }

    private JPanel createBrand() {
        JPanel panel = Ui.panel(new Color(0x12, 0x12, 0x12));
        panel.setPreferredSize(new Dimension(430, 0));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(54, 0, 38, 0));

        CuteMovieLogo logo = new CuteMovieLogo(220);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(logo);
        panel.add(Box.createVerticalStrut(20));

        JLabel title = Ui.heading("光影票务", 32);
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(10));

        JLabel subtitle = Ui.label("电影票售票系统", new Color(0xCB, 0xD1, 0xDA));
        subtitle.setFont(Ui.font(Font.PLAIN, 14));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(subtitle);
        return panel;
    }

    private JPanel createFormArea() {
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(LIGHT_BG);

        formStack.setLayout(new java.awt.CardLayout(0, 0));
        formStack.setOpaque(false);
        formStack.add(createLoginForm(), "login");
        formStack.add(createRegisterForm(), "register");

        JPanel wrapper = Ui.panel(LIGHT_BG);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.add(Box.createVerticalGlue());
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(formStack, BorderLayout.CENTER);
        wrapper.add(center);
        wrapper.add(Box.createVerticalGlue());
        right.add(wrapper, BorderLayout.CENTER);
        return right;
    }

    private JPanel createLoginForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(22, 20, 30, 20));
        form.setPreferredSize(new Dimension(420, 490));
        form.setMaximumSize(new Dimension(420, 490));

        JLabel title = Ui.heading("登录", 28);
        title.setForeground(DARK_TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(title);
        form.add(Box.createVerticalStrut(28));

        form.add(fieldLabel("用户名"));
        form.add(Box.createVerticalStrut(7));
        form.add(fixed(loginUser));
        form.add(Box.createVerticalStrut(15));
        form.add(fieldLabel("密码"));
        form.add(Box.createVerticalStrut(7));
        form.add(fixed(loginPassword));
        form.add(Box.createVerticalStrut(12));
        form.add(errorLabel(messageLabel));
        form.add(Box.createVerticalStrut(10));

        var loginButton = Ui.primary("登录");
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        setAction(loginButton, event -> doLogin());
        form.add(fixed(loginButton));
        form.add(Box.createVerticalStrut(14));

        // 用户名/密码输入框内按回车都能直接提交
        bindEnter(loginUser, this::doLogin);
        bindEnter(loginPassword, this::doLogin);

        var registerButton = Ui.textButton("创建账号", Ui.PRIMARY);
        registerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerButton.addActionListener(event -> {
            messageLabel.setText(" ");
            show("register");
        });
        form.add(registerButton);
        return form;
    }

    private JPanel createRegisterForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(22, 20, 30, 20));
        form.setPreferredSize(new Dimension(420, 590));
        form.setMaximumSize(new Dimension(420, 590));

        JLabel title = Ui.heading("创建账号", 28);
        title.setForeground(DARK_TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(title);
        form.add(Box.createVerticalStrut(22));

        form.add(fieldLabel("用户名"));
        form.add(Box.createVerticalStrut(7));
        form.add(fixed(registerUser));
        form.add(Box.createVerticalStrut(13));
        form.add(fieldLabel("姓名"));
        form.add(Box.createVerticalStrut(7));
        form.add(fixed(registerName));
        form.add(Box.createVerticalStrut(13));
        form.add(fieldLabel("手机号"));
        form.add(Box.createVerticalStrut(7));
        form.add(fixed(registerPhone));
        form.add(Box.createVerticalStrut(13));
        form.add(fieldLabel("密码"));
        form.add(Box.createVerticalStrut(7));
        form.add(fixed(registerPassword));
        form.add(Box.createVerticalStrut(13));
        form.add(fieldLabel("确认密码"));
        form.add(Box.createVerticalStrut(7));
        form.add(fixed(registerConfirm));
        form.add(Box.createVerticalStrut(10));
        form.add(errorLabel(registerMessage));
        form.add(Box.createVerticalStrut(8));

        var submit = Ui.primary("注册并登录");
        submit.setAlignmentX(Component.LEFT_ALIGNMENT);
        setAction(submit, event -> doRegister());
        form.add(fixed(submit));
        form.add(Box.createVerticalStrut(12));

        // 注册表单所有输入框都支持回车提交
        for (JTextField field : new JTextField[]{registerUser, registerName,
                registerPhone, registerPassword, registerConfirm}) {
            bindEnter(field, this::doRegister);
        }

        var back = Ui.textButton("返回登录", DARK_TEXT);
        back.setAlignmentX(Component.LEFT_ALIGNMENT);
        back.addActionListener(event -> {
            registerMessage.setText(" ");
            show("login");
        });
        form.add(back);
        return form;
    }

    private void doLogin() {
        messageLabel.setForeground(Ui.DANGER);
        messageLabel.setText(" ");
        clearInvalid(loginUser, loginPassword);

        String username = loginUser.getText().trim();
        String password = new String(loginPassword.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            if (username.isEmpty()) {
                markInvalid(loginUser);
            }
            if (password.isEmpty()) {
                markInvalid(loginPassword);
            }
            messageLabel.setText(username.isEmpty() ? "请输入用户名和密码" : "请输入密码");
            (username.isEmpty() ? loginUser : loginPassword).requestFocusInWindow();
            return;
        }

        try {
            Account account = service.login(username, password);
            if (account == null) {
                messageLabel.setText("用户名或密码错误");
                return;
            }
            onAuthenticated.accept(account);
        } catch (DemoException ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    private void doRegister() {
        registerMessage.setForeground(Ui.DANGER);
        registerMessage.setText(" ");
        clearInvalid(registerUser, registerName, registerPhone, registerPassword, registerConfirm);

        String username = registerUser.getText().trim();
        String name = registerName.getText().trim();
        String phone = registerPhone.getText().trim();
        String password = new String(registerPassword.getPassword());
        String confirm = new String(registerConfirm.getPassword());

        if (username.isEmpty()) {
            markInvalid(registerUser);
            registerMessage.setText("请输入用户名");
            registerUser.requestFocusInWindow();
            return;
        }
        if (name.isEmpty()) {
            markInvalid(registerName);
            registerMessage.setText("请输入姓名");
            registerName.requestFocusInWindow();
            return;
        }
        if (phone.isEmpty()) {
            markInvalid(registerPhone);
            registerMessage.setText("请输入手机号");
            registerPhone.requestFocusInWindow();
            return;
        }
        if (password.isEmpty()) {
            markInvalid(registerPassword);
            registerMessage.setText("请输入密码");
            registerPassword.requestFocusInWindow();
            return;
        }
        if (!password.equals(confirm)) {
            markInvalid(registerPassword);
            markInvalid(registerConfirm);
            registerMessage.setText("两次输入的密码不一致");
            registerConfirm.requestFocusInWindow();
            return;
        }
        try {
            service.register(username, name, phone, password);
            onAuthenticated.accept(service.login(username, password));
        } catch (DemoException ex) {
            registerMessage.setText(ex.getMessage());
        }
    }

    /**
     * 给输入框加红色描边，表示该字段校验未通过。
     *
     * <p>与 {@link #clearInvalid} 成对使用：提交前先清空所有标记，
     * 再按实际校验结果重新标记，避免上一次的红框残留。</p>
     */
    private static void markInvalid(javax.swing.text.JTextComponent field) {
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Ui.DANGER),
                BorderFactory.createEmptyBorder(7, 11, 7, 11)));
    }

    private static void clearInvalid(javax.swing.text.JTextComponent... fields) {
        for (javax.swing.text.JTextComponent field : fields) {
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(INPUT_BORDER),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        }
    }

    private void show(String card) {
        java.awt.CardLayout layout = (java.awt.CardLayout) formStack.getLayout();
        layout.show(formStack, card);
        revalidate();
        repaint();
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = Ui.fieldLabel(text);
        label.setForeground(DARK_TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel errorLabel(JLabel label) {
        label.setForeground(Ui.DANGER);
        label.setFont(Ui.font(Font.PLAIN, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JTextField field(int columns) {
        JTextField field = new JTextField(columns);
        styleInput(field);
        return field;
    }

    private static JPasswordField password(int columns) {
        JPasswordField field = new JPasswordField(columns);
        styleInput(field);
        return field;
    }

    private static void styleInput(javax.swing.text.JTextComponent component) {
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        component.setOpaque(true);
        component.setBackground(Color.WHITE);
        component.setForeground(DARK_TEXT);
        component.setCaretColor(DARK_TEXT);
        component.setFont(Ui.FONT);
    }

    /**
     * 让回车键在输入框内直接提交表单。
     *
     * <p>Swing 默认不会把回车转发给按钮，必须显式绑定。这里用
     * {@code JTextField.addActionListener}（{@link JPasswordField} 继承自它），
     * 而不是 {@code getRootPane().setDefaultButton()} —— 本面板会被放进
     * {@code MovieTicketDemoApp} 的 CardLayout 中，挂载前拿不到 rootPane。</p>
     *
     * @param field  明文或密码输入框
     * @param submit 回车后执行的动作
     */
    private static void bindEnter(JTextField field, Runnable submit) {
        field.addActionListener(event -> submit.run());
    }

    private static Component fixed(javax.swing.JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension size = component.getPreferredSize();
        component.setPreferredSize(new Dimension(350, Math.max(size.height, 42)));
        component.setMaximumSize(new Dimension(350, Math.max(size.height, 42)));
        return component;
    }

    private static void setAction(javax.swing.JButton button, ActionListener listener) {
        button.addActionListener(listener);
    }
}
