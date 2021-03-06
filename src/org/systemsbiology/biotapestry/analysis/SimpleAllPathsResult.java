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

package org.systemsbiology.biotapestry.analysis;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Collections;


/****************************************************************************
**
** All paths between two nodes
*/

public class SimpleAllPathsResult {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private String srcID_;
  private String targID_;
  private TreeMap<String, Integer> nodes_;
  private HashSet<SignedLink> links_;
  private SimplePathTracker tracker_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public SimpleAllPathsResult(String srcID, String targID) {
    srcID_ = srcID;
    targID_ = targID;
    nodes_ = new TreeMap<String, Integer>();
    links_ = new HashSet<SignedLink>();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
        
  /***************************************************************************
  **
  ** Add a node
  */
    
  public void addNode(String nodeID) {
    nodes_.put(nodeID, new Integer(0));
    return;
  }

  /***************************************************************************
  **
  ** Set a depth
  */
    
  public void setDepth(String nodeID, int depth) {
    nodes_.put(nodeID, new Integer(depth));
    return;
  }  
  
  /***************************************************************************
  **
  ** Add a link
  */
    
  public void addLink(SignedLink link) {
    links_.add(link);
  }
    
  /***************************************************************************
  **
  ** Get the depth of a node
  */
  
  public Integer getDepth(String nodeID) {
    return (nodes_.get(nodeID));
  }     

  /***************************************************************************
  **
  ** Get the links
  */
  
  public Iterator<SignedLink> getLinks() {
    return (links_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get the number of paths links
  */
  
  public int getLinkSetSize() {
    return (links_.size());
  }  

  /***************************************************************************
  **
  ** Get the links
  */
  
  public Set<SignedLink> getLinkSet() {
    return (Collections.unmodifiableSet(links_));
  }  

  /***************************************************************************
  **
  ** Get the nodes
  */
  
  public Iterator<String> getNodes() {
    return (nodes_.keySet().iterator());
  } 
  
  /***************************************************************************
  **
  ** Get the source of the paths
  */
  
  public String getSource() {
    return (srcID_);
  } 
  
  /***************************************************************************
  **
  ** Get the target of the paths
  */
  
  public String getTarget() {
    return (targID_);
  }   

  /***************************************************************************
  **
  ** Get a list of path descriptions
  */
  
  public List<SimplePath> getPaths() {
    ArrayList<SimplePath> retval = new ArrayList<SimplePath>();
    Iterator<SimplePath> paths = tracker_.getPaths();
    while (paths.hasNext()) {
      SimplePath path = paths.next();
      retval.add(path.clone());
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Set the tracker results
  */
  
  public void setTracker(SimplePathTracker tracker) {
    tracker_ = tracker;
    Iterator<SimplePath> paths = tracker.getPaths();
    while (paths.hasNext()) {
      SimplePath path =paths.next();
      Iterator<SignedLink> lit = path.pathIterator();
      while (lit.hasNext()) {
        SignedLink link = lit.next();
        addLink(link);
        addNode(link.getSrc());
      }
    }
    addNode(targID_);
    return;
  }  
  
  /***************************************************************************
  **
  ** Standard toString
  */
  
  public String toString() {
    return ("SimpleAllPathsResult source = " + srcID_ +
                                           " target = " + targID_ +
                                           " links = " + links_ +
                                           " nodes = " + nodes_);
  }  
}
