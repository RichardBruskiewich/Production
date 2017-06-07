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

package org.systemsbiology.biotapestry.cmd;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Map;

import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.HarnessBuilder;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.add.AddNetModule;
import org.systemsbiology.biotapestry.cmd.flow.add.PullDown;
import org.systemsbiology.biotapestry.cmd.flow.move.Mover;
import org.systemsbiology.biotapestry.cmd.flow.move.RunningMove;
import org.systemsbiology.biotapestry.cmd.flow.move.RunningMoveGenerator;
import org.systemsbiology.biotapestry.cmd.flow.select.Selection;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.TextBoxManager;
import org.systemsbiology.biotapestry.ui.menu.XPlatMaskingStatus;

/***************************************************************************
** 
** These commands handle mouse events for the model display
*/

public class PanelCommands  {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  //
  // Add mode cancel options:
  //
  
  public static final int CANCEL_ADDS_ALL_MODES        = 0x00;
  public static final int CANCEL_ADDS_SKIP_PULLDOWNS   = 0x01;  
  public static final int CANCEL_ADDS_SKIP_MODULE_ADDS = 0x02; 

  //
  // Programmatic enable:
  //
  
  public static final int ENABLE_NONE        = 0x00;
  public static final int ENABLE_PULLDOWNS   = 0x01;  
  public static final int ENABLE_MODULE_ADDS = 0x02;
   
  //
  // Operational modes:
  //
  
  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum Mode {  
    NO_MODE,
    ADD_GENE,
    DRAW_NET_MODULE,
    DRAW_NET_MODULE_LINK,
    ADD_TO_NET_MODULE,
    DRAW_GROUP,
    MOVE_GROUP,
    MOVE_NET_MODULE,
    ADD_LINK,
    ADD_NODE,
    RELOCATE,
    CHANGE_SOURCE_NODE,
    CHANGE_TARGET_NODE,
    ADD_NOTE,
    ADD_SUB_GROUP,
    RELOCATE_TARGET,
    RELOCATE_SOURCE,
    SWAP_PADS,
    ADD_EXTRA_PROXY_NODE,
    PULL_DOWN,
    PULL_DOWN_ROOT_INSTANCE,
    RELOCATE_NET_MOD_LINK,
    RELOCATE_NET_MOD_TARGET,
    RELOCATE_NET_MOD_SOURCE, 
    CHANGE_GROUP_MEMBERSHIP,
    PATHS_FROM_USER_SELECTED,
    DEFINE_CIS_REG_MODULE,
    ;
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private ModeHandler currentClickHandler_;
  private ModeHandler noModeHandler_;
  private Mode currentMode_;
  private ServerControlFlowHarness harness_;
  private boolean moveResult_;
  
  private UIComponentSource uics_;
  private DynamicDataAccessContext ddacx_;
  private HarnessBuilder hBld_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Constructor
  */
  
  public PanelCommands(UIComponentSource uics, DynamicDataAccessContext ddacx, HarnessBuilder hBld) {
    uics_ = uics;
    ddacx_ = ddacx;
    hBld_ = hBld;
    currentClickHandler_ = null;
    noModeHandler_ = new DefaultHandler();
    currentMode_ = Mode.NO_MODE;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
   
  public void setHarness(ServerControlFlowHarness harness) {
    harness_ = harness;
    return;
  }
  
  /***************************************************************************
  **
  ** Main entry point
  */
   
  public void processMouseClick(Point pt, boolean isShifted, StaticDataAccessContext rcx) {
    PanelCommands.ModeHandler handler = currentClickHandler_;
    handler.handleMouseClick(pt, isShifted, rcx);
    return;
  }
  
  /***************************************************************************
  **
  ** Main entry point too
  */ 
  
  public void processMouseMotion(Point pt, StaticDataAccessContext rcx) {
    ((MotionHandler)currentClickHandler_).handleMouseMotion(pt, rcx);
    return;
  }
 
  /***************************************************************************
  **
  ** Answer if current mode shows bubbles.
  */
     
  public boolean isInShowBubbleMode() {
    if (currentClickHandler_ == null) {
      return (false);
    }
    return (currentClickHandler_.showBubbleHandler());
  }
  
  /***************************************************************************
  **
  ** Answer if we are modal
  */
  
  public boolean isModal() {
    return (currentMode_ != Mode.NO_MODE);
  }  
  
  /***************************************************************************
  **
  ** Answer if we show module components for mode
  */
  
  public boolean showModuleComponentsForMode() { 
    return (currentMode_ == PanelCommands.Mode.MOVE_NET_MODULE);
  }
  
  /***************************************************************************
  **
  ** Answer if drag is not allowed for mode
  */
  
  public boolean dragNotAllowedForMode() { 
    return (currentMode_ == PanelCommands.Mode.ADD_LINK);
  }
  
  /***************************************************************************
  **
  ** Cancel the add mode (modal toggles are optional)
  */

  public void cancelAddMode(int which) {
    if (!uics_.getCommonView().canAccept()) {
      return;
    }
    uics_.getTextBoxMgr().clearCurrentMouseOver();
    uics_.getTextBoxMgr().clearMessageSource(TextBoxManager.SELECTED_ITEM_MESSAGE);   
    if (isInShowBubbleMode()) {
      uics_.getSUPanel().popBubbles();
    }
    ModeHandler handler = currentClickHandler_;
    if (handler != null) {
      handler.cancelMode(null);
    }
    if (harness_ != null) {
      harness_.clearFlow();
    }
    currentMode_ = Mode.NO_MODE;
    currentClickHandler_ = noModeHandler_;
    
    uics_.getCommonView().clearPullFrame(); 
    uics_.getGenomePresentation().setFloater(null);
    uics_.getGenomePresentation().clearTargets();
    uics_.getGenomePresentation().clearRootOverlays();    
    uics_.getCursorMgr().showDefaultCursor();
    uics_.getCommonView().enableControls();
    uics_.getCommonView().cancelModals(which);
    uics_.getSUPanel().drawModel(false);
    return;
  }
  
  /***************************************************************************
  **
  ** Set the mode
  */
  
  public boolean setToMode(DialogAndInProcessCmd daipc, XPlatMaskingStatus ms) {
    PanelCommands.ModeHandler handler = getNewHandler(daipc);
    Mode newMode = handler.setToMode(daipc,  ms);
    if (newMode == Mode.NO_MODE) {
      return (false);
    }
    currentClickHandler_ = handler;
    currentMode_ = newMode;
    return (true);
  }  
  
  /***************************************************************************
  **
  ** Get the current mode handler
  */
    
   public ModeHandler getCurrentHandler(boolean readOnly) {
     return ((readOnly || (currentClickHandler_ == null)) ? noModeHandler_ : currentClickHandler_);
   }
     
  /***************************************************************************
  **
  ** Select items in a rectangle
  */
    
   public void selectItems(Rectangle rect, boolean isShifted) {
     HarnessBuilder.PreHarness pH = hBld_.buildHarness(FlowMeister.OtherFlowKey.LOCAL_RECT_SELECTION);
     Selection.StepState agis = (Selection.StepState)pH.getCmdState();
     agis.setPreload(rect, isShifted);
     hBld_.runHarness(pH);
     return;
   }
 
  /***************************************************************************
  **
  ** Handle the visualization of a move (used to modify a drag layout)
  */
   
  public void visualizeAMove(RunningMove rmov, Point pt0, Layout dragLayout) {  
    Layout.PadNeedsForLayout padFixups = null; 
    RunningMove.PadFixup needFixups = RunningMoveGenerator.needPadFixupsForMove(rmov);
    StaticDataAccessContext rcx = (new StaticDataAccessContext(ddacx_)).replaceImmediateLayout(dragLayout);
    if (needFixups != RunningMove.PadFixup.NO_PAD_FIXUP) {
      padFixups = dragLayout.findAllNetModuleLinkPadRequirementsForOverlay(rcx);
    }
    Mover.StepState.moveItem(rmov, pt0, rcx, padFixups);
    if (padFixups != null) {
      Map<String, Boolean> orpho = dragLayout.orphansOnlyForAll(false);
      dragLayout.repairAllNetModuleLinkPadRequirements(rcx, padFixups, orpho);
    }
    return;
  }
    
  /***************************************************************************
  **
  ** Bundle a move
  */
   
  public boolean doAMove(RunningMove rmov, Point2D end) {
    moveResult_ = false;
    HarnessBuilder.PreHarness pH = hBld_.buildHarness(FlowMeister.OtherFlowKey.MOVE_ELEMENTS);
    Mover.StepState agis = (Mover.StepState)pH.getCmdState();
    agis.setPreload(rmov, end, this);
    hBld_.runHarness(pH);
    return (moveResult_);
  }
  
  /***************************************************************************
  **
  ** report result out-of-band
  */
   
  public void setMoveResult(boolean ok) {
    moveResult_ = ok;
    return ;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Classes to handle mode actions
  */
  
  public abstract class ModeHandler {
    
    public abstract Mode setToMode(DialogAndInProcessCmd daipc, XPlatMaskingStatus ms);   
    public abstract void cancelMode(Object args);
    public abstract void handleMouseClick(Point pt, boolean isShifted, StaticDataAccessContext rcx);
    
    protected boolean modelIsSubset() {
      String genomeKey = ddacx_.getCurrentGenomeID();  
      Genome genome = ddacx_.getGenomeSource().getGenome(genomeKey);    
      if (genome instanceof GenomeInstance) {
        GenomeInstance parent = ((GenomeInstance)genome).getVfgParent();
        if (parent != null) {
          return (true);
        }
      }
      return (false);
    } 
    
    protected boolean modelIsRoot() {
      String genomeKey = ddacx_.getCurrentGenomeID();  
      Genome genome = ddacx_.getGenomeSource().getGenome(genomeKey);    
      return (genome instanceof DBGenome);
    }
       
    protected boolean modelIsInstance() {
      String genomeKey = ddacx_.getCurrentGenomeID();
      Genome genome = ddacx_.getGenomeSource().getGenome(genomeKey);    
      return (genome instanceof GenomeInstance);
    }
    
    protected boolean modelIsDynamic() {
      String genomeKey = ddacx_.getCurrentGenomeID();
      Genome genome = ddacx_.getGenomeSource().getGenome(genomeKey);    
      return (genome instanceof DynamicGenomeInstance);
    }
 
    public boolean floaterIsRect() {
      return (false);
    }
    
    public boolean floaterIsLines() {
      return (false);
    }
    
    public boolean isMoveHandler() {
      return (false);
    }
    
    public boolean isPullDownHandler() {
      return (false);
    }
        
    public boolean showBubbleHandler() {
      return (false);
    }    
     
    @SuppressWarnings("unused")
    public Object getFloater(int x, int y) {
      return (null);
    }
    
    public Color getFloaterColor() {
      return (null);
    }
  } 
  
  /***************************************************************************
  **
  ** Mode Handler
  */ 
    
  public class DrawHandler extends ModeHandler {
      
    protected DialogAndInProcessCmd.MouseClickCmdState mccs;
    protected Mode myMode;
     
    protected DrawHandler(DialogAndInProcessCmd daipc) {
      myMode = daipc.suPanelMode;
      mccs = (DialogAndInProcessCmd.MouseClickCmdState)daipc.currStateX;
    }
     
    public void handleMouseClick(Point pt, boolean isShifted, StaticDataAccessContext rcx) {
      ServerControlFlowHarness.ClickResult result = ServerControlFlowHarness.ClickResult.UNSELECTED;
      try {    
        DialogAndInProcessCmd daipc = harness_.handleClick(pt, isShifted, rcx.getPixDiam());
        result = daipc.pccr;
      } finally {
        if ((result == ServerControlFlowHarness.ClickResult.PROCESSED) || (result == ServerControlFlowHarness.ClickResult.CANCELLED)) {
          DialogAndInProcessCmd.MouseClickCmdState.Floater flstate = mccs.floaterMode();
          boolean hasFloater = (flstate != DialogAndInProcessCmd.MouseClickCmdState.Floater.NO_FLOATER);
          setToNoMode(result, mccs.showBubbles(), mccs.hasTargetsAndOverlays(), hasFloater);
          mccs = null;
        }
      }
      if ((result == ServerControlFlowHarness.ClickResult.REJECT) || 
          (result == ServerControlFlowHarness.ClickResult.ERROR) || 
          (result == ServerControlFlowHarness.ClickResult.UNSELECTED)) {
        uics_.getSUPanel().giveErrorFeedback();
      } else {
        uics_.getSUPanel().drawModel(false);
      } 
      return;
    }

    public void cancelMode(Object args) {
      mccs = null;
      return;
    }

    @Override
    public Object getFloater(int x, int y) {
      DialogAndInProcessCmd.MouseClickCmdState.Floater flstate = mccs.floaterMode();
      if (flstate != DialogAndInProcessCmd.MouseClickCmdState.Floater.NO_FLOATER) {
        return (mccs.getFloater(x, y));
      }
      return (null);
    }
    
    @Override
    public boolean showBubbleHandler() {
      return ((mccs != null) && mccs.showBubbles());
    }  
    
    @Override
    public boolean floaterIsRect() {
      return ((mccs != null) && (mccs.floaterMode() == DialogAndInProcessCmd.MouseClickCmdState.Floater.RECT_FLOATER));
    }
    
    @Override
    public boolean floaterIsLines() {
      return ((mccs != null) && (mccs.floaterMode() == DialogAndInProcessCmd.MouseClickCmdState.Floater.LINES_FLOATER));
    }

    public Mode setToMode(DialogAndInProcessCmd daipc, XPlatMaskingStatus ms) {
      uics_.getTextBoxMgr().clearCurrentMouseOver();
      uics_.getTextBoxMgr().clearMessageSource(TextBoxManager.SELECTED_ITEM_MESSAGE);
      if (mccs.noSubModels() && modelIsSubset()) {
        return (Mode.NO_MODE);  // nothing to do for subset stuff
      }
      if (mccs.noInstances() && modelIsInstance()) {
        return (Mode.NO_MODE);  // nothing to do for instance stuff
      }
      if (mccs.mustBeDynamic() && !modelIsDynamic()) {
       return (Mode.NO_MODE);
      }  
      if (mccs.noRootModel() && modelIsRoot()) {
       return (Mode.NO_MODE);
      }     
      if (mccs.cannotBeDynamic() && modelIsDynamic()) {
       return (Mode.NO_MODE);
      }

      //
      // Somewhat legacy; if the flow is supposed to return an object floater, and it is null, that 
      // indicates a user cancel or an immediate add for some cases (e.g. a preexisting node in a submodel):
      //
      
      DialogAndInProcessCmd.MouseClickCmdState.Floater flstate = mccs.floaterMode();
      Object newFloater = null;
      if (flstate == DialogAndInProcessCmd.MouseClickCmdState.Floater.OBJECT_FLOATER) {
        newFloater = mccs.getFloater(0, 0);
        if (newFloater == null) {
          mccs = null;
          return (Mode.NO_MODE);
        }
      }
  
      if (mccs.showBubbles()) {
        uics_.getSUPanel().pushBubblesAndShow();
      }
      uics_.getCommonView().disableControls(ms);
      
      if (flstate == DialogAndInProcessCmd.MouseClickCmdState.Floater.OBJECT_FLOATER) {
        uics_.getGenomePresentation().setFloater(newFloater);
        Layout flay = uics_.getGenomePresentation().getFloaterLayout();
        mccs.setFloaterPropsInLayout(flay);
      } else {
        uics_.getGenomePresentation().setFloater(null); // Includes LINES_FLOATER case as well (set via another route)   
      }
      uics_.getCursorMgr().showModeCursor();
      if (mccs.hasTargetsAndOverlays()) {
        uics_.getSUPanel().drawModel(false);
      }
      //this.requestFocus();  this may be useful?? 
      return (myMode);
    }    
  }
  
    
  /***************************************************************************
  **
  ** Mode Handler
  */
  
  public class DrawNetModuleHandler extends ModeHandler {    
    
    private AddNetModule.StepState anm_;
    protected Mode myMode;
     
    protected DrawNetModuleHandler(DialogAndInProcessCmd daipc) {
      myMode = daipc.suPanelMode;
      anm_ = (AddNetModule.StepState)daipc.currStateX;
    }
    
    public void handleMouseClick(Point pt, boolean isShifted, StaticDataAccessContext rcx) {
       ServerControlFlowHarness.ClickResult result = ServerControlFlowHarness.ClickResult.ACCEPT;
       try {    
         result = harness_.handleClick(pt, isShifted, rcx.getPixDiam()).pccr;
       } finally {
         if (result == ServerControlFlowHarness.ClickResult.REJECT) {
           uics_.getSUPanel().giveErrorFeedback();       
         } else {
           if ((anm_.nmc_ != null) && (anm_.nmc_.buildState == AddNetModule.CommonNetModuleCandidate.DONE)) {
             boolean timeToBail = true;
             if (anm_.nmc_.keepGoing) {
               anm_.nmc_.changeToAdd();
               timeToBail = false;
             }
             if (timeToBail) {
               setToNoMode(result, false, false, true);
               anm_ = null;
             }
           }
         }
       }
       uics_.getSUPanel().drawModel(false);
       return;
     }
    
    public void cancelMode(Object args) {
      anm_ = null;
      return;
    }
  
    @Override
    public boolean floaterIsRect() {
      return (anm_.floaterMode() == DialogAndInProcessCmd.MouseClickCmdState.Floater.RECT_FLOATER);
    }

    @Override
    public boolean floaterIsLines() {
      return (anm_.floaterMode() == DialogAndInProcessCmd.MouseClickCmdState.Floater.LINES_FLOATER);
    }
     
    @Override
    public Object getFloater(int x, int y) {
      return (anm_.getFloater(x, y));
    }
    
    @Override
    public Color getFloaterColor() {
      if (anm_ != null) {
        return (anm_.getFloaterColor());
      }
      return (null);
    }   
     
    public Mode setToMode(DialogAndInProcessCmd daipc, XPlatMaskingStatus ms) {
      uics_.getTextBoxMgr().clearCurrentMouseOver();
      uics_.getTextBoxMgr().clearMessageSource(TextBoxManager.SELECTED_ITEM_MESSAGE);   
      anm_ = (AddNetModule.StepState)daipc.currStateX; 
      uics_.getCommonView().disableControls(ms);
      uics_.getGenomePresentation().setFloater(null);        
      uics_.getCursorMgr().showModeCursor();
      //this.requestFocus();  this may be useful
      return (myMode);
    }
  }
 
  /***************************************************************************
  **
  ** Mode Handler
  */
  
  public class AddToNetModuleHandler extends ModeHandler {    
    
    private AddNetModule.StepState anm_;
    protected Mode myMode;
     
    protected AddToNetModuleHandler(DialogAndInProcessCmd daipc) {
      myMode = daipc.suPanelMode;
      anm_ = (AddNetModule.StepState)daipc.currStateX;
    }
    
    public void handleMouseClick(Point pt, boolean isShifted, StaticDataAccessContext rcx) {
       ServerControlFlowHarness.ClickResult result = ServerControlFlowHarness.ClickResult.ACCEPT;
       try {    
         result = harness_.handleClick(pt, isShifted, rcx.getPixDiam()).pccr;
       } finally {
         if (result == ServerControlFlowHarness.ClickResult.REJECT) {
           uics_.getSUPanel().giveErrorFeedback();
         } else { // THIS HANDLER _NEVER_ EXITS BY ITSELF!
           if (anm_.nmc_.buildState == AddNetModule.CommonNetModuleCandidate.DONE) {
             anm_.nmc_.reset();
           }
         }
       }
       uics_.getSUPanel().drawModel(false);
       return;
     }
      
    public void cancelMode(Object args) {
      anm_ = null;
      return;
    }
    
    @Override
    public boolean floaterIsRect() {
      return (anm_.floaterMode() == DialogAndInProcessCmd.MouseClickCmdState.Floater.RECT_FLOATER);
    }
    
    @Override
    public boolean floaterIsLines() {
      return (anm_.floaterMode() == DialogAndInProcessCmd.MouseClickCmdState.Floater.LINES_FLOATER);
    }
    
    @Override
    public Object getFloater(int x, int y) {
      return (anm_.getFloater(x, y));
    }
    
    @Override
    public Color getFloaterColor() {
      if (anm_ != null) {
        return (anm_.getFloaterColor());
      }
      return (null);
    }   
    
    public Mode setToMode(DialogAndInProcessCmd daipc, XPlatMaskingStatus ms) {
      uics_.getTextBoxMgr().clearCurrentMouseOver();
      uics_.getTextBoxMgr().clearMessageSource(TextBoxManager.SELECTED_ITEM_MESSAGE);   
      anm_ = (AddNetModule.StepState)daipc.currStateX;
      uics_.getCommonView().disableControls(ms);
      uics_.getGenomePresentation().setFloater(null);        
      uics_.getCursorMgr().showModeCursor();
      //this.requestFocus();  this may be useful
      return (myMode);
    }    
  }
 
  /***************************************************************************
  **
  ** Mode Handler
  */     
    
   public class PullDownHandler extends ModeHandler {
    
    @SuppressWarnings("unused")
    private PullDown.StepState dpss_;
    protected Mode myMode;
     
    protected PullDownHandler(DialogAndInProcessCmd daipc) {
      myMode = daipc.suPanelMode;
      dpss_ = (PullDown.StepState)daipc.currStateX;
    }

    public void handleMouseClick(Point pt, boolean isShifted, StaticDataAccessContext rcx) {
      ServerControlFlowHarness.ClickResult result = ServerControlFlowHarness.ClickResult.UNSELECTED;
      try {    
        result = harness_.handleClick(pt, isShifted, rcx.getPixDiam()).pccr;
      } finally {
        if ((result == ServerControlFlowHarness.ClickResult.REJECT) || (result == ServerControlFlowHarness.ClickResult.ERROR) || (result == ServerControlFlowHarness.ClickResult.UNSELECTED)) {
          uics_.getSUPanel().giveErrorFeedback();
        } 
      }
      uics_.getSUPanel().drawModel(false); 
      return;
    }
    
    @Override
    public boolean isPullDownHandler() {
      return (true);
    }
    
    public void cancelMode(Object args) {
      dpss_ = null;
      return;
    }    
       
    public Mode setToMode(DialogAndInProcessCmd daipc, XPlatMaskingStatus ms) {
      uics_.getTextBoxMgr().clearCurrentMouseOver();
      uics_.getTextBoxMgr().clearMessageSource(TextBoxManager.SELECTED_ITEM_MESSAGE);   
      if (!modelIsInstance()) {
        return (Mode.NO_MODE); 
      }
      uics_.getCommonView().disableControls(ms);
      if (modelIsSubset()) {
        uics_.getCursorMgr().showModeCursor();
      }
      //this.requestFocus();  this may be useful
      return (myMode);
    }
  }
   
  /***************************************************************************
  **
  ** Mode Handler
  */ 
    
  public class SUFHHandler extends ModeHandler {
      
    protected DialogAndInProcessCmd.MouseClickCmdState mccs;
    protected Mode myMode;
     
    protected SUFHHandler(DialogAndInProcessCmd daipc) {
      myMode = daipc.suPanelMode;
      mccs = (DialogAndInProcessCmd.MouseClickCmdState)daipc.currStateX;
    }
     
    public void handleMouseClick(Point pt, boolean isShifted, StaticDataAccessContext rcx) {
      ServerControlFlowHarness.ClickResult result = ServerControlFlowHarness.ClickResult.ACCEPT;
      try {    
        result = harness_.handleClick(pt, isShifted, rcx.getPixDiam()).pccr;
      } finally {
        if (result == ServerControlFlowHarness.ClickResult.REJECT) {
          uics_.getSUPanel().giveErrorFeedback();
        } else { // Note this gets called if we had exception emerge from handleClick: result still ACCEPT
          setToNoMode(result, mccs.showBubbles(), false, false);
        }
      }
      uics_.getSUPanel().drawModel(false);
      return;
    }
   
    @Override
    public boolean showBubbleHandler() {
      return (mccs.showBubbles());
    }   
  
    public void cancelMode(Object args) {
      return;
    }

    public Mode setToMode(DialogAndInProcessCmd daipc, XPlatMaskingStatus ms) {
      uics_.getTextBoxMgr().clearCurrentMouseOver();
      uics_.getTextBoxMgr().clearMessageSource(TextBoxManager.SELECTED_ITEM_MESSAGE);
      if (mccs.noSubModels() && modelIsSubset()) {
        return (Mode.NO_MODE);  // nothing to do for subset stuff
      }
      if (mccs.noInstances() && modelIsInstance()) {
        return (Mode.NO_MODE);  // nothing to do for instance stuff
      }
      if (mccs.mustBeDynamic() && !modelIsDynamic()) {
       return (Mode.NO_MODE);
      }  
      if (mccs.noRootModel() && modelIsRoot()) {
       return (Mode.NO_MODE);
      }     
      if (mccs.cannotBeDynamic() && modelIsDynamic()) {
       return (Mode.NO_MODE);
      }         
      if (mccs.showBubbles()) {
        uics_.getSUPanel().pushBubblesAndShow();
      }   
      uics_.getCommonView().disableControls(ms);     
      uics_.getCursorMgr().showModeCursor();
      return (myMode);
    }    
  }
  
  /***************************************************************************
  **
  ** Mode Handler used for defining cis-reg module; needs to run for two clicks
  */ 
    
  public class MultiClickHandler extends ModeHandler {
      
    protected DialogAndInProcessCmd.MouseClickCmdState mccs;
    protected Mode myMode;
     
    protected MultiClickHandler(DialogAndInProcessCmd daipc) {
      myMode = daipc.suPanelMode;
      mccs = (DialogAndInProcessCmd.MouseClickCmdState)daipc.currStateX;
    }
      
    public void handleMouseClick(Point pt, boolean isShifted, StaticDataAccessContext rcx) {
      ServerControlFlowHarness.ClickResult result = ServerControlFlowHarness.ClickResult.ACCEPT;
      try {    
        result = harness_.handleClick(pt, isShifted, rcx.getPixDiam()).pccr;
      } finally {
        if (result == ServerControlFlowHarness.ClickResult.REJECT) {
         uics_.getSUPanel().giveErrorFeedback();       
        } else if ((result == ServerControlFlowHarness.ClickResult.PROCESSED) || (result == ServerControlFlowHarness.ClickResult.CANCELLED)) {
          setToNoMode(result, mccs.showBubbles(), false, false);
          mccs = null;
        }
      }
      uics_.getSUPanel().drawModel(false);
      return;
    }
   
    @Override
    public boolean showBubbleHandler() {
      return (mccs.showBubbles());
    }   
  
    public void cancelMode(Object args) {
      return;
    }

    public Mode setToMode(DialogAndInProcessCmd daipc, XPlatMaskingStatus ms) {
      uics_.getTextBoxMgr().clearCurrentMouseOver();
      uics_.getTextBoxMgr().clearMessageSource(TextBoxManager.SELECTED_ITEM_MESSAGE);
      if (mccs.noSubModels() && modelIsSubset()) {
        return (Mode.NO_MODE);  // nothing to do for subset stuff
      }
      if (mccs.noInstances() && modelIsInstance()) {
        return (Mode.NO_MODE);  // nothing to do for instance stuff
      }
      if (mccs.mustBeDynamic() && !modelIsDynamic()) {
       return (Mode.NO_MODE);
      }  
      if (mccs.noRootModel() && modelIsRoot()) {
       return (Mode.NO_MODE);
      }     
      if (mccs.cannotBeDynamic() && modelIsDynamic()) {
       return (Mode.NO_MODE);
      }         
      if (mccs.showBubbles()) {
        uics_.getSUPanel().pushBubblesAndShow();
      }   
      uics_.getCommonView().disableControls(ms);     
      uics_.getCursorMgr().showModeCursor();
      return (myMode);
    }    
  }
  
  /***************************************************************************
  **
  ** Move Handler
  */
  
  public class MotionHandler extends ModeHandler {        

    private Mover.StepState mvss_;
    protected Mode myMode;
       
    protected MotionHandler(DialogAndInProcessCmd daipc) {
      myMode = daipc.suPanelMode;
      mvss_ = (Mover.StepState)daipc.currStateX;
    }
     
    public void handleMouseClick(Point pt, boolean isShifted, StaticDataAccessContext rcx) {
      ServerControlFlowHarness.ClickResult result = ServerControlFlowHarness.ClickResult.ACCEPT;
      try {    
        result = harness_.handleClick(pt, isShifted, rcx.getPixDiam()).pccr;
      } finally {
        if (result == ServerControlFlowHarness.ClickResult.REJECT) {
          uics_.getSUPanel().giveErrorFeedback();
        } else { // Note this gets called if we had exception emerge from handleClick: result still ACCEPT
          setToNoMode(result, false, false, true);
        }
      }
      uics_.getSUPanel().drawModel(false);
      return;
    }
    
    @Override
    public boolean isMoveHandler() {
      return (true);
    }
    
    public RunningMove[] getMultiMov() {
      return (mvss_.getMultiMov());
    }   
    
    public void handleMouseMotion(Point pt, StaticDataAccessContext rcx) {
      mvss_.handleMouseMotion(pt, rcx);
      return;
    }  
    
    public void cancelMode(Object args) {
      mvss_ = null;
      return;
    }
      
    public Mode setToMode(DialogAndInProcessCmd daipc, XPlatMaskingStatus ms) {
      uics_.getTextBoxMgr().clearCurrentMouseOver();
      uics_.getTextBoxMgr().clearMessageSource(TextBoxManager.SELECTED_ITEM_MESSAGE);   
      uics_.getCommonView().disableControls(ms);
      return (myMode); 
    }
  }

  /***************************************************************************
  **
  ** Mode Handler
  */     
    
  public class DefaultHandler extends ModeHandler { 
    
    //
    // This handler does things differently, since it is _always_ running (unless another mode handler is installed), 
    // and we do not want a long-term control flow. SO the click launches a new flow, instead of sending the click info
    // to an already running flow.
    //
    
    public void handleMouseClick(Point pt, boolean isShifted, StaticDataAccessContext rcx) {
      HarnessBuilder.PreHarness pH = hBld_.buildHarness(FlowMeister.OtherFlowKey.LOCAL_CLICK_SELECTION, rcx);
      Selection.StepState agis = (Selection.StepState)pH.getCmdState();
      agis.setPreload(pt, rcx.getPixDiam(), isShifted);
      hBld_.runHarness(pH);
      return;
    }
        
    public void cancelMode(Object args) {
      return;
    }    
  
    public Mode setToMode(DialogAndInProcessCmd daipc, XPlatMaskingStatus ms) {
      return (Mode.NO_MODE); 
    }        
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // LOCAL METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  void setToNoMode(ServerControlFlowHarness.ClickResult result, boolean popBub, boolean targsAndOvers, boolean clearFloat) {
    currentMode_ = Mode.NO_MODE;
    currentClickHandler_ = noModeHandler_;
    if (popBub) {
      uics_.getSUPanel().popBubbles();
    }
    if (result != ServerControlFlowHarness.ClickResult.ACCEPT_DELAYED) {
      uics_.getCommonView().enableControls();
    }
    if (clearFloat) {
      uics_.getGenomePresentation().setFloater(null);
    }
    if (targsAndOvers) {
      uics_.getGenomePresentation().clearTargets();
      uics_.getGenomePresentation().clearRootOverlays();
    }
    uics_.getCursorMgr().showDefaultCursor();
    return;
  }
   
  /***************************************************************************
  **
  ** Get the right handler
  */
  
  private ModeHandler getNewHandler(DialogAndInProcessCmd daipc) {
    switch (daipc.suPanelMode) {
    case NO_MODE: return new DefaultHandler(); 
    case ADD_GENE: return new DrawHandler(daipc);   
    case DRAW_NET_MODULE: return new DrawNetModuleHandler(daipc);    
    case DRAW_NET_MODULE_LINK: return new DrawHandler(daipc);       
    case ADD_TO_NET_MODULE: return new AddToNetModuleHandler(daipc);        
    case DRAW_GROUP: return new DrawHandler(daipc);       
    case MOVE_GROUP: return new MotionHandler(daipc);     
    case MOVE_NET_MODULE: return new MotionHandler(daipc);     
    case ADD_LINK: return new DrawHandler(daipc);      
    case ADD_NODE: return new DrawHandler(daipc);    
    case RELOCATE: return new SUFHHandler(daipc);      
    case CHANGE_SOURCE_NODE: return new SUFHHandler(daipc);       
    case CHANGE_TARGET_NODE: return new SUFHHandler(daipc);       
    case ADD_NOTE: return new DrawHandler(daipc);      
    case ADD_SUB_GROUP: return new SUFHHandler(daipc);       
    case RELOCATE_TARGET: return new SUFHHandler(daipc);              
    case RELOCATE_SOURCE: return new SUFHHandler(daipc);       
    case SWAP_PADS: return new SUFHHandler(daipc);      
    case ADD_EXTRA_PROXY_NODE: return new DrawHandler(daipc);     
    case PULL_DOWN: return new PullDownHandler(daipc);  
    case PULL_DOWN_ROOT_INSTANCE: return new PullDownHandler(daipc);
    case RELOCATE_NET_MOD_LINK: return new SUFHHandler(daipc);
    case RELOCATE_NET_MOD_TARGET: return new SUFHHandler(daipc);
    case RELOCATE_NET_MOD_SOURCE: return new SUFHHandler(daipc);    
    case CHANGE_GROUP_MEMBERSHIP: return  new SUFHHandler(daipc); 
    case PATHS_FROM_USER_SELECTED: return  new SUFHHandler(daipc);
    case DEFINE_CIS_REG_MODULE: return  new MultiClickHandler(daipc);
    default:
      throw new IllegalArgumentException();
    }
  }  
}
