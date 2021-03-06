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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Iterator;

import org.systemsbiology.biotapestry.plugin.InternalLinkDataDisplayPlugIn;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.plugin.PluginCallbackWorker;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Displays Time course data
*/

public class TimeCourseLinkDisplayPlugIn implements InternalLinkDataDisplayPlugIn {
  
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

  public TimeCourseLinkDisplayPlugIn() {
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
    return (InternalLinkDataDisplayPlugIn.IS_INTERNAL_ANSWER);
  } 
  
  /***************************************************************************
  **
  ** Answers if this plugin will require a per-instance display (as opposed to
  ** e.g. a single data window for a gene that is shared by all instances)
  */
  
  public boolean requiresPerInstanceDisplay(String genomeID, String itemID) {
    return (true);
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
  
  public PluginCallbackWorker getCallbackWorker(String genomeID, String linkID) {
    return (null);
  } 
  
  /***************************************************************************
  **
  ** Show the Time course data
  */
  
  public String getDataAsHTML(String genomeID, String linkID) {
    StringBuffer buf = new StringBuffer();
    Database db = appState_.getDB();
    Genome genome = db.getGenome(genomeID);
    Linkage link = genome.getLinkage(linkID);
    String targID = link.getTarget();
    String sourceID = link.getSource();
    String baseTargID = GenomeItemInstance.getBaseID(targID);
    String baseSourceID = GenomeItemInstance.getBaseID(sourceID);
    
    TimeCourseData tcd = db.getTimeCourseData();
    if ((tcd == null) || !tcd.haveData()) {
      return ("");
    }
    
    boolean haveHierarchy = tcd.hierarchyIsSet();
    DisplayOptions dOpt = appState_.getDisplayOptMgr().getDisplayOptions();
    boolean showTree = (haveHierarchy) ? dOpt.showExpressionTableTree() : false;
        
    ResourceManager rMan = appState_.getRMan();
    buf.append("<p></p>");   
    buf.append("<center><h1>");
    buf.append(rMan.getString("dataWindow.timeCourseDataForLink"));
    buf.append("</h1>\n");
    
    int needKey = TimeCourseTableDrawer.NO_TABLE_KEY;
    List<TimeCourseData.TCMapping> dataKeys = tcd.getTimeCourseTCMDataKeysWithDefault(baseSourceID);
    boolean gotData = false;
    if (dataKeys != null) {
      Iterator<TimeCourseData.TCMapping> dkit = dataKeys.iterator();   
      while (dkit.hasNext()) {
        TimeCourseData.TCMapping tcm = dkit.next();
        TimeCourseGene tcg = tcd.getTimeCourseDataCaseInsensitive(tcm.name);
        if (tcg == null) {  // If no table at all, still get back default name...
          continue;
        }
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        int nextKey = tcg.getExpressionTable(out, tcd, showTree);
        needKey |= nextKey;
        String tab = sw.getBuffer().toString();
        if ((tab != null) && (!tab.trim().equals(""))) {
          gotData = true;
          buf.append("<p>");
          buf.append(tab);
          buf.append("</p>");          
        }
      }
    }
    
    dataKeys = tcd.getTimeCourseTCMDataKeysWithDefault(baseTargID);
    if (dataKeys != null) {
      Iterator<TimeCourseData.TCMapping> dkit = dataKeys.iterator();
      while (dkit.hasNext()) {
        TimeCourseData.TCMapping tcm = dkit.next();
        TimeCourseGene tcg = tcd.getTimeCourseDataCaseInsensitive(tcm.name);
        if (tcg == null) {  // If no table at all, still get back default name...
          continue;
        }
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        int nextKey = tcg.getExpressionTable(out, tcd, showTree);
        needKey |= nextKey;
        String tab = sw.getBuffer().toString();
        if ((tab != null) && (!tab.trim().equals(""))) {
          gotData = true;
          buf.append("<p>");
          buf.append(tab);
          buf.append("</p>");          
        }
      }
    }
    
    buf.append(TimeCourseTableDrawer.buildKey(appState_, needKey, showTree, false));

    if (!gotData) {
      buf.append(rMan.getString("dataWindow.noExpressionProfile"));
    }
    buf.append("</center>");
      
    return (buf.toString());
  }
}
