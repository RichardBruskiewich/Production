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


package org.systemsbiology.biotapestry.cmd.undo;

import org.systemsbiology.biotapestry.app.NavigationChange;
import org.systemsbiology.biotapestry.db.DataAccessContext;

/****************************************************************************
**
** Handles undos of navigation trees
*/

public class NavigationChangeCmd extends BTUndoCmd {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private NavigationChange restore_;

  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Build the command
  */ 
  
  public NavigationChangeCmd(DataAccessContext dacx, NavigationChange restore) {
    super(dacx);
    restore_ = restore;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Answer if the command has permanent effects
  */ 
  
  @Override
  public boolean changesModel() {
    return (false);
  }
    
  /***************************************************************************
  **
  ** Name to show
  */ 
  
  @Override
  public String getPresentationName() {
    return ("Navigation Change");
  }

  /***************************************************************************
  **
  ** Undo the operation
  */ 
  
  @Override
  public void undo() { 
    super.undo();
    uics_.getCommonView().changeUndo(restore_, dacx_); 
    return;
  }  
 
  /***************************************************************************
  **
  ** Redo the operation
  */ 
  
  @Override
  public void redo() {
    super.redo();
    uics_.getCommonView().changeRedo(restore_, dacx_); 
    return;
  }
}
