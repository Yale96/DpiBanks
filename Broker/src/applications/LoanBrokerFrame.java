/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package applications;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import messaging.requestreply.RequestReply;

import model.bank.*;
import model.loan.LoanReply;
import model.loan.LoanRequest;
import org.apache.activemq.ActiveMQConnectionFactory;

/**
 *
 * @author Frank
 */
public class LoanBrokerFrame extends JFrame {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private DefaultListModel<JListLine> listModel = new DefaultListModel<JListLine>();
    private JList<JListLine> list;

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    LoanBrokerFrame frame = new LoanBrokerFrame();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public LoanBrokerFrame() {
        setTitle("Loan Broker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 450, 300);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        GridBagLayout gbl_contentPane = new GridBagLayout();
        gbl_contentPane.columnWidths = new int[]{46, 31, 86, 30, 89, 0};
        gbl_contentPane.rowHeights = new int[]{233, 23, 0};
        gbl_contentPane.columnWeights = new double[]{1.0, 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
        gbl_contentPane.rowWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
        contentPane.setLayout(gbl_contentPane);

        JScrollPane scrollPane = new JScrollPane();
        GridBagConstraints gbc_scrollPane = new GridBagConstraints();
        gbc_scrollPane.gridwidth = 7;
        gbc_scrollPane.insets = new Insets(0, 0, 5, 5);
        gbc_scrollPane.fill = GridBagConstraints.BOTH;
        gbc_scrollPane.gridx = 0;
        gbc_scrollPane.gridy = 0;
        contentPane.add(scrollPane, gbc_scrollPane);

        list = new JList<JListLine>(listModel);
        scrollPane.setViewportView(list);

        new Thread(new Consumer()).start();
        new Thread(new ConsumerTwo()).start();

    }

    private JListLine getRequestReply(LoanRequest request) {

        for (int i = 0; i < listModel.getSize(); i++) {
            JListLine rr = listModel.get(i);
            if (rr.getLoanRequest() == request) {
                return rr;
            }
        }

        return null;
    }

    private JListLine getBankRequestReply(BankInterestRequest request) {

        for (int i = 0; i < listModel.getSize(); i++) {
            JListLine rr = listModel.get(i);
            if (rr.getBankRequest().getHash().equals(request.getHash())) {
                return rr;
            }
        }

        return null;
    }

    public void add(LoanRequest loanRequest) {
        listModel.addElement(new JListLine(loanRequest));
    }

    public void add(LoanRequest loanRequest, BankInterestRequest bankRequest) {
        JListLine rr = getRequestReply(loanRequest);
        if (rr != null && bankRequest != null) {
            rr.setBankRequest(bankRequest);
            list.repaint();
        }
    }

    public void add(LoanRequest loanRequest, BankInterestReply bankReply) {
        JListLine rr = getRequestReply(loanRequest);
        if (rr != null && bankReply != null) {
            rr.setBankReply(bankReply);
            list.repaint();
        }
    }

    public class Consumer implements Runnable {

        @Override
        public void run() {
            for (;;) {
                try {
                    ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
                    connectionFactory.setTrustAllPackages(true);
                    Connection connection = connectionFactory.createConnection();
                    connection.start();
                    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    Destination destination = session.createQueue("LoanRequest.Client");
                    MessageConsumer consumer = session.createConsumer(destination);
                    Message message = consumer.receive(1000);

                    if (message instanceof ObjectMessage) {
                        Object object = ((ObjectMessage) message).getObject();
                        RequestReply rr = (RequestReply) object;
                        LoanRequest loanRequest = (LoanRequest) rr.getRequest();
                        add(loanRequest);
                        BankInterestRequest bankInterestRequest = new BankInterestRequest(loanRequest.getAmount(), loanRequest.getTime());
                        bankInterestRequest.setHash(loanRequest.getHash());
                        add(loanRequest, bankInterestRequest);
                        RequestReply rTwo = new RequestReply<BankInterestRequest, BankInterestReply>(bankInterestRequest, null);
                        forward(rTwo);
                    }

                    consumer.close();
                    session.close();
                    connection.close();
                } catch (JMSException e) {
                    System.out.println("Caught: " + e);
                }
            }
        }
    }

    public class ConsumerTwo implements Runnable {

        @Override
        public void run() {
            for (;;) {
                try {
                    ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
                    connectionFactory.setTrustAllPackages(true);
                    Connection connection = connectionFactory.createConnection();
                    connection.start();
                    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    Destination destination = session.createQueue("LoanReply.Bank");
                    MessageConsumer consumer = session.createConsumer(destination);
                    Message message = consumer.receive(1000);

                    if (message instanceof ObjectMessage) {
                        Object object = ((ObjectMessage) message).getObject();
                        RequestReply rr = (RequestReply) object;
                        BankInterestRequest bir = (BankInterestRequest) rr.getRequest();
                        BankInterestReply bire = (BankInterestReply)rr.getReply();

                        JListLine jls = getBankRequestReply(bir);
                        add(jls.getLoanRequest(), bire);
                        LoanReply lr = new LoanReply();
                        lr.setInterest(bire.getInterest());
                        lr.setHash(bire.getHash());
                        lr.setQuoteID(bire.getQuoteId());
                        RequestReply rr2 = new RequestReply(jls.getLoanRequest(), lr);
                        
                        forwardTwo(rr2);
                    }

                    consumer.close();
                    session.close();
                    connection.close();
                } catch (JMSException e) {
                    System.out.println("Caught: " + e);
                }
            }
        }
    }

    public void forward(RequestReply rr) {
        try {
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
            connectionFactory.setTrustAllPackages(true);
            Connection connection = connectionFactory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue("LoanRequest.Broker");
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            ObjectMessage objectMessage = session.createObjectMessage();
            objectMessage.setObject(rr);
            producer.send(objectMessage);
            System.out.println(rr.getRequest().toString());
            session.close();
            connection.close();
        } catch (JMSException e) {
            System.out.println("Caught: " + e);
        }
    }
    
    public void forwardTwo(RequestReply rr) {
        try {
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
            connectionFactory.setTrustAllPackages(true);
            Connection connection = connectionFactory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue("LoanReply.Broker");
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            ObjectMessage objectMessage = session.createObjectMessage();
            objectMessage.setObject(rr);
            producer.send(objectMessage);
            System.out.println(rr.getRequest().toString());
            session.close();
            connection.close();
        } catch (JMSException e) {
            System.out.println("Caught: " + e);
        }
    }
}
