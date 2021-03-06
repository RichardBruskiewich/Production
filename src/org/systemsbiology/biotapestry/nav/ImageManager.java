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

package org.systemsbiology.biotapestry.nav;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.UniqueLabeller;
import org.systemsbiology.biotapestry.util.Base64Util;

/****************************************************************************
**
** Image Manager.  This is a Singleton.
*/

public class ImageManager {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static final int WARN_DIM_X = 200;
  public static final int WARN_DIM_Y = 200;  
  
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

  private HashMap<String, String> stringCache_;
  private HashMap<String, BufferedImage> imageCache_;
  private HashMap<String, String> typeDictionary_;
  private HashMap<String, Integer> imgCounts_;  
  private UniqueLabeller labels_;
  private StringBuffer buildBuf_;
  private String bufKey_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */
    
  public ImageManager() {
    stringCache_ = new HashMap<String, String>();
    imageCache_ = new HashMap<String, BufferedImage>();
    typeDictionary_ = new HashMap<String, String>();
    imgCounts_ = new HashMap<String, Integer>();
    labels_ = new UniqueLabeller();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** clear out the image library
  */
    
  public void dropAllImages() {
    stringCache_.clear();
    imageCache_.clear();
    typeDictionary_.clear();
    imgCounts_.clear();
    labels_ = new UniqueLabeller();
    return;
  }  
   
  /***************************************************************************
  ** 
  ** Get the image associated with the given key
  */

  public BufferedImage getImage(String key) {
    if (key == null) {
      return (null);
    }
    BufferedImage bi = imageCache_.get(key);
    if (bi == null) {
      String imgString = stringCache_.get(key);
      String type = typeDictionary_.get(key);
      try {
        bi = readImageFromString(imgString, type);
        imageCache_.put(key, bi);
      } catch (IOException ioex) {
        throw new IllegalStateException();
      }
    }
    return (bi);
  } 
  
  /***************************************************************************
  ** 
  ** Get the image type associated with the given key
  */

  public String getImageType(String key) {
    if (key == null) {
      return (null);
    }
    BufferedImage bi = getImage(key);
    if (bi != null) {
      return (typeDictionary_.get(key));
    } 
    return (null);
  } 
  
  /***************************************************************************
  ** 
  ** Answers if we have an image
  */

  public boolean hasAnImage() {
    Iterator<Integer> icit = imgCounts_.values().iterator();
    while (icit.hasNext()) {
      Integer count = icit.next();
      if (count.intValue() > 0) {
        return (true);
      }
    }
    return (false);
  }  

  /***************************************************************************
  ** 
  ** Register that we are using the image
  */

  public ImageChange registerImageUsage(String key) {
    Integer currCount = imgCounts_.get(key);
    ImageChange retval = new ImageChange();
    retval.countOnlyKey = key;
    retval.oldCount = currCount.intValue();
    retval.newCount = retval.oldCount + 1;
    imgCounts_.put(key, new Integer(retval.newCount));  
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Register that we are dropping the image
  */

  public ImageChange dropImageUsage(String key) {
    Integer currCount = imgCounts_.get(key);
    ImageChange retval = new ImageChange();
    if (currCount.intValue() == 1) {
      retval.oldImage = stringCache_.get(key);
      retval.oldType = typeDictionary_.get(key);
      retval.oldCount = 1;
      retval.oldKey = key;
      stringCache_.remove(key);
      imageCache_.remove(key);
      typeDictionary_.remove(key);
      imgCounts_.remove(key);  
      labels_.removeLabel(key);
    } else {
      retval.oldCount = currCount.intValue();
      retval.newCount = retval.oldCount - 1;
      retval.countOnlyKey = key;
      imgCounts_.put(key, new Integer(retval.newCount));
    }
    return (retval);
  }  

  /***************************************************************************
  ** 
  ** Load in an image from a file.  Return a key with the change info
  */

  public TypedImage loadImageFromFileStart(File loadFile) throws IOException {
    TypedImage timg = readImageFromFile(loadFile);
    return (timg);
  }   
 
  /***************************************************************************
  ** 
  ** Load in an image from a file.  Return a key with the change info
  */

  public NewImageInfo loadImageFromFileFinish(TypedImage timg) throws IOException {
    String imgString = writeImageToString(timg);
    NewImageInfo retval = new NewImageInfo();

    //
    // Look for a match in the cache:
    //
    
    Iterator<String> skit = stringCache_.keySet().iterator();
    while (skit.hasNext()) {
      String key = skit.next();
      String cacheString = stringCache_.get(key);
      if (cacheString.equals(imgString)) {  // Image already in use
        // count is incremented later.  Nothing to do!
        retval.key = key;
        retval.change = null;
        return (retval);
      }
    }  
      
    //
    // No match:
    //
      
    String imgKey = labels_.getNextLabel();
    imageCache_.put(imgKey, timg.img);
    stringCache_.put(imgKey, imgString);
    typeDictionary_.put(imgKey, timg.type);
    imgCounts_.put(imgKey, new Integer(0));
    retval.key = imgKey;
    retval.change = new ImageChange();
    retval.change.newImage = imgString;
    retval.change.newKey = imgKey;
    retval.change.newType = timg.type;
    retval.change.newCount = 0;
    return (retval);
  } 
  
  /***************************************************************************
  ** 
  ** Add to an image definition
  */

  public void appendToImageDefinition(String key, char[] chars, int start, int length) {   
    if (buildBuf_ == null) {
      buildBuf_ = new StringBuffer();
    }
    if ((bufKey_ == null) || !bufKey_.equals(key)) {
      throw new IllegalStateException();
    }
    buildBuf_.append(chars, start, length);
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Finish an image definition
  */

  public void finishImageDefinition(String key) {
    if ((bufKey_ == null) || !bufKey_.equals(key)) {
      throw new IllegalStateException();
    }
     bufKey_ = null;
    if (buildBuf_ == null) {
      return;
    }
     // "Trim" the character array:
    int bufLen = buildBuf_.length();
    int index = bufLen - 1;
    while (Character.isWhitespace(buildBuf_.charAt(index))) {
      buildBuf_.setLength(index--);
    }
    index = 0;
    while (Character.isWhitespace(buildBuf_.charAt(index))) {
      buildBuf_.deleteCharAt(index++);
    }
    // Turn it into a String:
    stringCache_.put(key, buildBuf_.toString());
    if (buildBuf_ != null) {
      buildBuf_.setLength(0);
    }
    return;
  }  
   
  /***************************************************************************
  **
  ** Write the image library to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.println("<images>");
    ind.up();
    TreeSet<String> keys = new TreeSet<String>(stringCache_.keySet());
    Iterator<String> skit = keys.iterator();
    while (skit.hasNext()) {
      String key = skit.next();
      String imgString = stringCache_.get(key);
      String imgType = typeDictionary_.get(key);
      Integer imgCount = imgCounts_.get(key);  
      ind.indent();
      out.print("<image ");
      out.print("key=\""); 
      out.print(key);
      out.print("\" refCount=\"");
      out.print(imgCount);
      out.print("\" encoding=\"");
      out.print(imgType);
      out.println("\" >");            
      out.println(imgString);
      ind.indent();
      out.println("</image>");
    }
    ind.down().indent();
    out.println("</images>");
    return;
  }

  /***************************************************************************
  **
  ** Undo an image change
  */
  
  public void changeUndo(ImageChange undo) {
    if (undo.newKey != null) {  // new Image was installed; remove it
      stringCache_.remove(undo.newKey);
      imageCache_.remove(undo.newKey);
      typeDictionary_.remove(undo.newKey);
      imgCounts_.remove(undo.newKey);  
      labels_.removeLabel(undo.newKey);
    } else if (undo.oldKey != null) {  // old image was removed; restore it
      labels_.addExistingLabel(undo.oldKey);
      stringCache_.put(undo.oldKey, undo.oldImage);
      typeDictionary_.put(undo.oldKey, undo.oldType);
      imgCounts_.put(undo.oldKey, new Integer(undo.oldCount));            
    } else {  // reference count changed
      imgCounts_.put(undo.countOnlyKey, new Integer(undo.oldCount));
    }
    return;
  }
    
  /***************************************************************************
  **
  ** Redo an image change
  */
  
  public void changeRedo(ImageChange redo) {
    if (redo.newKey != null) {  // new Image was installed; re-install
      labels_.addExistingLabel(redo.newKey);
      stringCache_.put(redo.newKey, redo.newImage);
      typeDictionary_.put(redo.newKey, redo.newType);
      imgCounts_.put(redo.newKey, new Integer(redo.newCount));
    } else if (redo.oldKey != null) {  // old image was removed; remove it again
      stringCache_.remove(redo.oldKey);
      imageCache_.remove(redo.oldKey);
      typeDictionary_.remove(redo.oldKey);
      imgCounts_.remove(redo.oldKey);  
      labels_.removeLabel(redo.oldKey);
    } else {  // reference count changed
      imgCounts_.put(redo.countOnlyKey, new Integer(redo.newCount));      
    }    
    return;
  }
  
  /***************************************************************************
  **
  ** Get supported import file suffixes
  */
  
  public List<String> getSupportedFileSuffixes() {
    String[] names = ImageIO.getReaderFormatNames();
    List<String> nameList = Arrays.asList(names);
    ArrayList<String> retval = new ArrayList<String>(); 
    if (nameList.contains("tif")) {
      retval.add("tif");
      retval.add("tiff");
    }
    if (nameList.contains("png")) {
      retval.add("png");
    }
    if (nameList.contains("jpg")) {
      retval.add("jpg");
      retval.add("jpeg");
    }
    if (nameList.contains("gif")) {
      retval.add("gif");
    }
    return (retval);
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("images");
    return (retval);
  }

  /***************************************************************************
  **
  ** Get image keyword
  **
  */
  
  public static String getImageKeyword() {
    return ("image");
  }
  
  /***************************************************************************
  **
  ** Handle image creation
  **
  */
  
  public static String installFromXML(BTState appState, String elemName, 
                                      Attributes attrs) throws IOException {
    String imgKey = null;
    String encodingStr = null;
    String countStr = null;
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("key")) {
          imgKey = val;
        } else if (key.equals("encoding")) {
          encodingStr = val;          
        } else if (key.equals("refCount")) {
          countStr = val;          
        }
      }
    }
    
    if ((encodingStr == null) || (imgKey == null) || (countStr == null)) {
      throw new IOException();
    }
    
    int count = 0;
    try {
      count = Integer.parseInt(countStr); 
    } catch (NumberFormatException nfe) {
      throw new IOException();
    }
    
    ImageManager mgr = appState.getImageMgr();
    mgr.stringCache_.put(imgKey, "");
    mgr.typeDictionary_.put(imgKey, encodingStr);
    mgr.imgCounts_.put(imgKey, new Integer(count));    
    mgr.labels_.addExistingLabel(imgKey);
    mgr.bufKey_ = imgKey;
    return (imgKey);
  }

  /***************************************************************************
  **
  ** Test frame. Having the sun.misc class references in the code base
  ** is bad.  So comment this out unless using it.
  **
  */  
  
  public static void main(String[] argv) {
    /*
    try {
     
      TypedImage ti = getMgr().readImageFromFile(new File("foo"));
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      Iterator writers = ImageIO.getImageWritersByFormatName(ti.type);
      if (!writers.hasNext()) {
        throw new IOException();
      }
      ImageWriter writer = (ImageWriter)writers.next();
      ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
      writer.setOutput(ios);
      IIOImage img = new IIOImage(ti.img, null, null);
      writer.write(img);
      ios.close();
      writer.dispose();
      byte[] inbytes = baos.toByteArray();
      //byte[] shortie = new byte[100];
      //System.arraycopy(inbytes, 0, shortie, 0, 100);
      String retval = (new Base64Util()).encode(inbytes);
      String retval1 = (new sun.misc.BASE64Encoder()).encode(inbytes);
      byte[] outbytes1 = (new Base64Util()).decode(retval);     
      byte[] outbytes = new sun.misc.BASE64Decoder().decodeBuffer(retval);
      for (int i = 0; i < retval.length(); i++) {
        if (retval.charAt(i) != retval1.charAt(i)) {
          System.out.println(i + ": " + retval.charAt(i) + " " + retval1.charAt(i));
        }
      }
      System.out.println("i = " + outbytes1.length + " o = " + outbytes.length);
      for (int i = 0; i < outbytes1.length; i++) {
        if (outbytes1[i] != outbytes[i]) {
          System.out.println(i + ": " + outbytes1[i] + " " + outbytes[i]);
        }
      }    
    } catch (Exception ex) {
      System.err.println("Caught exception " + ex.getMessage());
    }
    */
  }    
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Used to transmit new key with an ImageChange
  */
  
  public static class NewImageInfo {
    public String key;
    public ImageChange change;
  }  
  
  /***************************************************************************
  **
  ** Used to transmit image and type info
  */
  
  public static class TypedImage {
    BufferedImage img;
    String type;
    
    public int getHeight() {
      return ((img == null) ? 0 : img.getHeight());     
    }
    
    public int getWidth() {
      return ((img == null) ? 0 : img.getWidth());     
    }
    
  }
  

  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  ** 
  ** Write out an image
  */

  private String writeImageToString(TypedImage timg) throws IOException {    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(timg.type);
    if (!writers.hasNext()) {
      throw new IOException();
    }
    ImageWriter writer = writers.next();
    ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
    writer.setOutput(ios);
    IIOImage img = new IIOImage(timg.img, null, null);
    writer.write(img);
    ios.close();
    writer.dispose();    
    String retval = (new Base64Util()).encode(baos.toByteArray());
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Read in an image.  Everybody except JPEG gets stored as PNG
  */

  private TypedImage readImageFromFile(File readFile) throws IOException { 
    FileInputStream fis = new FileInputStream(readFile);
    ImageInputStream iis = ImageIO.createImageInputStream(fis);
    Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
    if (!readers.hasNext()) {     
      throw new IOException();
    }
    TypedImage retval = new TypedImage();
    ImageReader reader = readers.next();
    retval.type = reader.getFormatName();
    if (retval.type.equalsIgnoreCase("jpg") || retval.type.equalsIgnoreCase("jpeg")) {
      retval.type = "jpg";
    } else {
      retval.type = "png";
    }
    // FIX ME: CMYK JPEGS not supported, but we don't catch this before the IO Exception (BT-10-25-10:1)
    retval.img = ImageIO.read(iis);   
    return (retval);
  }
 
  /***************************************************************************
  ** 
  ** Read in an image from a string
  */

  private BufferedImage readImageFromString(String imgString, String type) throws IOException {
    byte[] bytes = (new Base64Util()).decode(imgString);    
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(type);
    if (!readers.hasNext()) {
      throw new IOException();
    }
    ImageReader reader = readers.next();
    ImageInputStream iis = ImageIO.createImageInputStream(bais);
    reader.setInput(iis);
    BufferedImage retval = reader.read(0);
    iis.close();
    reader.dispose();
    return (retval); 
  }
}
