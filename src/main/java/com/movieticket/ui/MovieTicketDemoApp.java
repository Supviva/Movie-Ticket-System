package com.movieticket.ui;

import com.movieticket.demo.DemoMovieTicketService;
import com.movieticket.model.Account;
import com.movieticket.model.Role;
import com.movieticket.ui.component.Ui;
import com.movieticket.ui.screen.AdminPanel;
import com.movieticket.ui.screen.CustomerPanel;
import com.movieticket.ui.screen.LoginPanel;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.EventQueue;

public final class MovieTicketDemoApp extends JFrame {

    private final DemoMovieTicketService service = new DemoMovieTicketService();
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel content = new JPanel(contentLayout);

    private MovieTicketDemoApp() {
        super("光影票务 · 电影票售票系统 Demo");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 760));
        setSize(1320, 840);
        setLocationRelativeTo(null);
        content.setLayout(contentLayout);
        setContentPane(content);
        showLogin();
    }

    private void showLogin() {
        content.removeAll();
        LoginPanel login = new LoginPanel(service, this::authenticated);
        content.add(login, "login");
        contentLayout.show(content, "login");
        content.revalidate();
        content.repaint();
    }

    private void authenticated(Account account) {
        content.removeAll();
        Runnable logout = this::showLogin;
        JPanel panel = account.role() == Role.ADMIN
                ? new AdminPanel(service, account, logout)
                : new CustomerPanel(service, account, logout);
        content.add(panel, account.role().name());
        contentLayout.show(content, account.role().name());
        content.revalidate();
        content.repaint();
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            Ui.installDefaults();
            new MovieTicketDemoApp().setVisible(true);
        });
    }
}
