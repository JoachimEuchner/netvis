package netvis.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import netvis.NetVisMain;

public class NetVisFrame extends  JFrame implements FocusListener, KeyListener
{
   /**
    * 
    */
   private static final long               serialVersionUID = 1L;

   private NetVisComponent mLVComponent;

   private final JPanel                    mNetVisPanel;
   
   private final JPanel                    mGraphCtrlPanel;
   
   private final JButton                   mIncreaseChargeButton;
   private final JButton                   mDecreaseChargeButton;
   private final JButton                   mIncreaseSpringButton;
   private final JButton                   mDecreaseSpringButton;
   
   public NetVisComponent getmNetVisComp()
   {
      return this.mLVComponent;
   }
   
   
   
   public NetVisFrame( NetVisMain m )
   {
      setLayout(new BorderLayout());
    
      
      this.mNetVisPanel = new JPanel();
      this.mNetVisPanel.setLayout(new BorderLayout());
      
      mLVComponent = new NetVisComponent( m, m.getNetVisModel() );
      this.mNetVisPanel.add( mLVComponent );
      
      this.add(mNetVisPanel, BorderLayout.CENTER);

      
      
      this.mGraphCtrlPanel = new JPanel();
      this.mGraphCtrlPanel.setLayout(new GridBagLayout());
      GridBagConstraints graphCtrlPanelConstraints = new GridBagConstraints();
      graphCtrlPanelConstraints.weightx = 1;
      graphCtrlPanelConstraints.weighty = 1;
      graphCtrlPanelConstraints.gridwidth = 1;
      graphCtrlPanelConstraints.gridheight = 1;
      graphCtrlPanelConstraints.gridx = 0;
      graphCtrlPanelConstraints.gridy = 0;
      
      this.mIncreaseChargeButton = new JButton("C++"); //$NON-NLS-1$
      this.mIncreaseChargeButton.addActionListener(this.getmNetVisComp());
      graphCtrlPanelConstraints.gridx = 0;
      this.mGraphCtrlPanel.add(this.mIncreaseChargeButton, graphCtrlPanelConstraints);
      
      this.mDecreaseChargeButton = new JButton("C--"); //$NON-NLS-1$
      this.mDecreaseChargeButton.addActionListener(this.getmNetVisComp());
      graphCtrlPanelConstraints.gridx = 1;
      this.mGraphCtrlPanel.add(this.mDecreaseChargeButton, graphCtrlPanelConstraints);
      
      this.mIncreaseSpringButton = new JButton("S++"); //$NON-NLS-1$
      this.mIncreaseSpringButton.addActionListener(this.getmNetVisComp());
      graphCtrlPanelConstraints.gridx = 2;
      this.mGraphCtrlPanel.add(this.mIncreaseSpringButton, graphCtrlPanelConstraints);
      
      this.mDecreaseSpringButton = new JButton("S--"); //$NON-NLS-1$
      this.mDecreaseSpringButton.addActionListener(this.getmNetVisComp());
      graphCtrlPanelConstraints.gridx = 3;
      this.mGraphCtrlPanel.add(this.mDecreaseSpringButton, graphCtrlPanelConstraints);
      
      this.add(mGraphCtrlPanel, BorderLayout.SOUTH);
      
      pack();
   }
   
   
   public NetVisComponent getNetVisComponent()
   {
      return mLVComponent;
   }
   
   
   @Override
   public void keyPressed(KeyEvent arg0)
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void keyReleased(KeyEvent arg0)
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void keyTyped(KeyEvent arg0)
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void focusGained(FocusEvent arg0)
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void focusLost(FocusEvent arg0)
   {
      // TODO Auto-generated method stub
      
   }


}
