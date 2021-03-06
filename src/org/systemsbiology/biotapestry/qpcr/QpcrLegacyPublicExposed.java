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

package org.systemsbiology.biotapestry.qpcr;

import java.io.PrintWriter;
import java.util.Map;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.parser.ParserClient;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.ui.DisplayOptions;

/****************************************************************************
**
** With the retirement of the QPCR package (except for legacy input and
** formatting the classic perturbation table presentation), all classes
** in the package EXCEPT THIS are now default (essentially package-only;
** we are not subclassing) visibility!  This ensure that all dependencies
** on the old code are limited to the following calls!
*/

public class QpcrLegacyPublicExposed {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private QPCRData legacyQPCR_;
  private QPCRData qpcrForDisplay_;
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public final static String LEGACY_BATCH_PREFIX = "_BT_";

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  public QpcrLegacyPublicExposed(BTState appState) {
    appState_ = appState;  
  }
  
  QpcrLegacyPublicExposed(QPCRData data) {
    legacyQPCR_ = data;
  }
    
  public ParserClient getParserClient(boolean mapsAreIllegal, boolean serialNumberIsIllegal) {
    return (new QpcrXmlFormatFactory(appState_, mapsAreIllegal, serialNumberIsIllegal));    
  }
   
  public boolean columnDefinitionsUsed() {
    return (qpcrForDisplay_.columnDefinitionsUsed());
  }
  
  public void transferFromLegacy() {
    legacyQPCR_.transferFromLegacy();
    return;
  }
  
  public void createQPCRFromPerts(PerturbationData pd) {
    QpcrDisplayGenerator qdg = new QpcrDisplayGenerator(appState_);
    qpcrForDisplay_ = qdg.createQPCRFromPerts(pd);
    return;
  }
  
  public boolean readyForDisplay() {
    return (qpcrForDisplay_ != null);
  }  
  
  public void dropCurrentStateForDisplay() {
    qpcrForDisplay_ = null;
    return;
  }
  
  public String getHTML(String geneId, String sourceID, boolean noCss, boolean bigScreen) {
    DisplayOptions dOpt = appState_.getDisplayOptMgr().getDisplayOptions();
    Map<String, String> colors = dOpt.getMeasurementDisplayColors();
    QpcrTablePublisher qtp = new QpcrTablePublisher(appState_, bigScreen, colors);
    return (qpcrForDisplay_.getHTML(geneId, sourceID, qtp));
  }
  
  public boolean publish(PrintWriter out) {
    DisplayOptions dOpt = appState_.getDisplayOptMgr().getDisplayOptions();
    Map<String, String> colors = dOpt.getMeasurementDisplayColors();
    QpcrTablePublisher qtp = new QpcrTablePublisher(appState_, colors);
    return (qtp.publish(out, qpcrForDisplay_));
  }    
}
  
 