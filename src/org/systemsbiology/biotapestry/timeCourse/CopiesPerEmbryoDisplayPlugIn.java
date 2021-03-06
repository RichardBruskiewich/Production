/*
**    Copyright (C) 2003-2013 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.timeCourse;

import java.text.MessageFormat;
import java.util.List;
import java.util.Iterator;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.plugin.InternalDataDisplayPlugInV2;
import org.systemsbiology.biotapestry.plugin.InternalNodeDataDisplayPlugIn;
import org.systemsbiology.biotapestry.plugin.PluginCallbackWorker;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Displays copies per embryo data
*/

public class CopiesPerEmbryoDisplayPlugIn implements InternalNodeDataDisplayPlugIn {
  
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public CopiesPerEmbryoDisplayPlugIn() {
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Internal plugins need to have access to internal state
  */
  
  public void setAppState(BTState appState) {
    appState_ = appState;
    return;
  }

  /***************************************************************************
  **
  ** Determine data requirements
  */
  
  public boolean isInternal() {
    return (InternalDataDisplayPlugInV2.IS_INTERNAL_ANSWER);
  } 
  
  /***************************************************************************
  **
  ** Answers if this plugin will require a per-instance display (as opposed to
  ** e.g. a single data window for a gene that is shared by all instances)
  */
  
  public boolean requiresPerInstanceDisplay(String genomeID, String itemID) {
    return (false);
  }
  
  /***************************************************************************
  **
  ** Answers if we can get back data later after retrieving it on a background thread.
  ** If true, the getDataAsHTML() routine should only provide the initial data view.
  */
  
  public boolean haveCallbackWorker() {
    return (false);
  }
 
  /***************************************************************************
  **
  ** Get the worker that will gather up background data and call us back
  */
  
  public PluginCallbackWorker getCallbackWorker(String genomeID, String nodeID) {
    return (null);
  }
    
  /***************************************************************************
  **
  ** Show the copies per embryo
  */
  
  public String getDataAsHTML(String genomeIDX, String nodeID) {
    StringBuffer buf = new StringBuffer(); 
    Database db = appState_.getDB(); 
    CopiesPerEmbryoData cped = db.getCopiesPerEmbryoData();
    if ((cped == null) || !cped.haveData()) {
      return ("");
    }
    
    // Always access from root model
    
    nodeID = GenomeItemInstance.getBaseID(nodeID);
       
    ResourceManager rMan = appState_.getRMan();
    Genome genome = db.getGenome();
    Node node = genome.getNode(nodeID);
    String title = node.getDisplayString(genome, false);    
    String useName = ((title == null) || (title.trim().equals(""))) ? "\" \"" : title;
    String format = rMan.getString("dataWindow.copiesPerEmbryoFor");
    buf.append("<center><h1>"); 
    buf.append(MessageFormat.format(format, new Object[] {useName})); 
    buf.append("</h1>\n");
    
    List<String> dataKeys = cped.getPerEmbryoCountDataKeysWithDefault(nodeID);
    boolean gotData = false;
    if (dataKeys != null) {
      Iterator<String> dkit = dataKeys.iterator();
      while (dkit.hasNext()) {
        String key = dkit.next();
        String tab = cped.getCountTable(key, appState_);
        if ((tab != null) && (!tab.trim().equals(""))) {
          gotData = true;
          buf.append("<p>");
          buf.append(tab);
          buf.append("</p>");          
        }
      }
    }
    if (!gotData) {
      buf.append(rMan.getString("dataWindow.noCopiesPerEmbryo"));
    }
    buf.append("</center>");
      
    return (buf.toString());
  }
}
