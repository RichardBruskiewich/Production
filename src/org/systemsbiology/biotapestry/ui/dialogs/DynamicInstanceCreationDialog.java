/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
**                            Seattle, Washington, USA. 
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/


package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;


/****************************************************************************
**
** Dialog box for creating dynamic genome instances
*/

public class DynamicInstanceCreationDialog extends JDialog {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JTextField nameField_;
  private JTextField minField_;
  private JTextField maxField_;  
  private JComboBox typeCombo_;
  private JComboBox skeyCombo_;
  private JCheckBox showDiffBox_;
  private JLabel skLabel_;
  private String nameResult_;
  private boolean isPerTimeResult_;
  private int minResult_;
  private int maxResult_;
  private boolean haveResult_;
  private int minTime_;
  private int maxTime_;
  private TimeAxisDefinition tad_;
  private boolean namedStages_;
  private boolean showDiffResult_;
  private String simKey_;
  private DataAccessContext dacx_; 
  private UIComponentSource uics_;
  
  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public DynamicInstanceCreationDialog(UIComponentSource uics, DataAccessContext dacx, int minTime, 
                                       int maxTime, boolean preferSum) {     
    super(uics.getTopFrame(), dacx.getRMan().getString("dicreate.title"), true);
    uics_ = uics;
    dacx_ = dacx;
    minTime_ = minTime;
    maxTime_ = maxTime;
    showDiffResult_ = false;
    simKey_ = null;
    ResourceManager rMan = dacx_.getRMan();    
    String defaultName = dacx_.getGenomeSource().getUniqueModelName(rMan);
    setSize(650, 200);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    
       
    tad_ = dacx_.getExpDataSrc().getTimeAxisDefinition();
    namedStages_ = tad_.haveNamedStages();
    String displayUnits = tad_.unitDisplayString();
    
    
    //
    // Build the Timely choice:
    //
    
    JLabel label = new JLabel(rMan.getString("dicreate.type"));
    Vector<String> choices = new Vector<String>();
    String perTime = MessageFormat.format(rMan.getString("dicreate.perTimeChoice"), new Object[] {displayUnits});
    choices.add(perTime);    
    choices.add(rMan.getString("dicreate.sumChoice"));
    typeCombo_ = new JComboBox(choices);
    if (preferSum) {
      typeCombo_.setSelectedIndex(1);
    }
    
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
    cp.add(label, gbc);
    
    UiUtil.gbcSet(gbc, 1, 0, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(typeCombo_, gbc);
    
    //
    // Build the minimum Time:
    //

    String minLab = MessageFormat.format(rMan.getString("dicreate.minTime"), new Object[] {displayUnits});    
    label = new JLabel(minLab);
    String initMin = (namedStages_) ? tad_.getNamedStageForIndex(minTime_).name : Integer.toString(minTime_);
    minField_ = new JTextField(initMin);
    UiUtil.gbcSet(gbc, 3, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(label, gbc);

    UiUtil.gbcSet(gbc, 4, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(minField_, gbc);
    
    //
    // Build the maximum Time:
    //

    String maxLab = MessageFormat.format(rMan.getString("dicreate.maxTime"), new Object[] {displayUnits});    
    label = new JLabel(maxLab);
    int initMax = (maxTime_ == -1) ? minTime_ + 1 : maxTime_;
    String initMaxStr = (namedStages_) ? tad_.getNamedStageForIndex(initMax).name : Integer.toString(initMax);
    maxField_ = new JTextField(initMaxStr);    
    UiUtil.gbcSet(gbc, 5, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(label, gbc);

    UiUtil.gbcSet(gbc, 6, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(maxField_, gbc);
    
    //
    // Build the name panel:
    //

    label = new JLabel(rMan.getString("dicreate.name"));
    nameField_ = new JTextField(defaultName);
    UiUtil.gbcSet(gbc, 0, 1, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(label, gbc);

    UiUtil.gbcSet(gbc, 1, 1, 6, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(nameField_, gbc); 
    
    
    label = new JLabel(rMan.getString("dicreate.showAsDiff"));
    showDiffBox_ = new JCheckBox();
   
    UiUtil.gbcSet(gbc, 0, 2, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
    cp.add(label, gbc);
    
    UiUtil.gbcSet(gbc, 1, 2, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(showDiffBox_, gbc);

    skLabel_ = new JLabel(rMan.getString("dicreate.simKeyOptions"));
    TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
    Vector<ObjChoiceContent> skeys = tcd.buildSimKeyCombo();
    skeyCombo_ = new JComboBox(skeys);
    UiUtil.gbcSet(gbc, 2, 2, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
    cp.add(skLabel_, gbc);
    
    UiUtil.gbcSet(gbc, 3, 2, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(skeyCombo_, gbc);
    skLabel_.setEnabled(false);
    skeyCombo_.setEnabled(false);  
    
    showDiffBox_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          boolean isSel = showDiffBox_.isSelected();
          skLabel_.setEnabled(isSel);
          skeyCombo_.setEnabled(isSel);
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });     
       
    //
    // Build the button panel:
    //
  
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (stashResults(true)) {
            DynamicInstanceCreationDialog.this.setVisible(false);
            DynamicInstanceCreationDialog.this.dispose();
          }
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.cancel"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try{
          stashResults(false);        
          DynamicInstanceCreationDialog.this.setVisible(false);
          DynamicInstanceCreationDialog.this.dispose();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonO);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonC);

    //
    // Build the dialog:
    //
    UiUtil.gbcSet(gbc, 0, 3, 7, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(uics_.getTopFrame());
  }


  /***************************************************************************
  **
  ** Get the result type (may be null)
  ** 
  */
  
  public boolean isPerTime() {
    return (isPerTimeResult_);
  } 
  
  /***************************************************************************
  **
  ** Get the minimum Time
  ** 
  */
  
  public int getMinTime() {
    return (minResult_);
  }  
  
  /***************************************************************************
  **
  ** Get the maxiumum Time
  ** 
  */
  
  public int getMaxTime() {
    return (maxResult_);
  }  
  
  /***************************************************************************
  **
  ** Get the name result (null = cancel).  May be a blank string
  ** 
  */
  
  public String getModName() {
    return (nameResult_);
  }
  
  /***************************************************************************
  **
  ** Answer if this is a show diff model
  ** 
  */
  
  public boolean getShowDiff() {
    return (showDiffResult_);
  } 
  
  /***************************************************************************
  **
  ** Sim Key for show diff model
  ** 
  */
  
  public String getSimKey() {
    return (simKey_);
  }  
   
  /***************************************************************************
  **
  ** Answer if we have a result
  ** 
  */
  
  public boolean haveResult() {
    return (haveResult_);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////


 
  /***************************************************************************
  **
  ** Stash our results for later interrogation.  If they have an error, pop
  ** up a warning dialog and return false, else return true.
  ** 
  */
  
  private boolean stashResults(boolean ok) {
    if (ok) {
      nameResult_ = nameField_.getText().trim();
      isPerTimeResult_ = (typeCombo_.getSelectedIndex() == 0);   
      if (namedStages_) {
        minResult_ = tad_.getIndexForNamedStage(minField_.getText());
        maxResult_ = tad_.getIndexForNamedStage(maxField_.getText());          
        if ((minResult_ == TimeAxisDefinition.INVALID_STAGE_NAME) ||
            (maxResult_ == TimeAxisDefinition.INVALID_STAGE_NAME)) {
          ResourceManager rMan = dacx_.getRMan(); 
          JOptionPane.showMessageDialog(this, rMan.getString("dicreate.badStageName"),
                                        rMan.getString("dicreate.badStageNameTitle"), 
                                        JOptionPane.ERROR_MESSAGE);
          haveResult_ = false;
          return (false);          
        }
      } else {      
        try {
          minResult_ = Integer.parseInt(minField_.getText());
          maxResult_ = Integer.parseInt(maxField_.getText());
        } catch (NumberFormatException nfex) {
          ResourceManager rMan = dacx_.getRMan(); 
          JOptionPane.showMessageDialog(this, rMan.getString("dicreate.badNumber"),
                                        rMan.getString("dicreate.badNumberTitle"), 
                                        JOptionPane.ERROR_MESSAGE);
          haveResult_ = false;
          return (false);
        }
      }
      if ((minResult_ < minTime_) || 
          ((maxTime_ != -1) && (maxResult_ > maxTime_)) ||
          (minResult_ > maxResult_)) {
        ResourceManager rMan = dacx_.getRMan(); 
        JOptionPane.showMessageDialog(this, rMan.getString("dicreate.badBounds"),
                                      rMan.getString("dicreate.badBoundsTitle"), 
                                      JOptionPane.ERROR_MESSAGE);
        haveResult_ = false;
        return (false);      
      }
      showDiffResult_ = showDiffBox_.isSelected();
      simKey_ = (showDiffResult_) ? ((ObjChoiceContent)skeyCombo_.getSelectedItem()).val : null;
      haveResult_ = true;
      return (true);
    } else {
      nameResult_ = null;      
      minResult_ = 0;      
      maxResult_ = 0;
      showDiffResult_ = false;
      simKey_ = null;
      isPerTimeResult_ = false;
      haveResult_ = false;
      return (true);
    }
  }    
}
