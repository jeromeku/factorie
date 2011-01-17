/* Copyright (C) 2008-2010 University of Massachusetts Amherst,
   Department of Computer Science.
   This file is part of "FACTORIE" (Factor graphs, Imperative, Extensible)
   http://factorie.cs.umass.edu, http://code.google.com/p/factorie/
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */

package cc.factorie
import java.io.{File,FileOutputStream,PrintWriter,FileReader,FileWriter,BufferedReader}

// For variables that hold one or more discrete value weights in a vector

/** A value consisting of one or more discrete values, representable as a vector. 
    Not that a single "DiscreteValue" is a subclass of this, represented as a "singleton vector",
    with 1.0 at the value's intValue and 0.0 everywhere else. */
trait DiscretesValue extends cc.factorie.la.Vector {
  def domain: DiscretesDomain
}

// TODO Consider changing to DiscretesDomain for parallel naming?
/** A Domain for variables whose value is a DiscretesValue, which is a Vector that also has a pointer back to its domain.
    This domain has a non-negative integer size.  The method 'size' is abstract. */
trait DiscretesDomain extends VectorDomain with ValueType[DiscretesValue] {
  /** The maximum size to which this domain will be allowed to grow.  
      The 'size' method may return values smaller than this, however.
      This method is used to pre-allocate a Template's parameter arrays and is useful for growing domains. */
  //def size: Int
  def dimensionSize: Int = dimensionDomain.size  // TODO Get rid of this?
  def dimensionDomain: DiscreteDomain
  def dimensionName(i:Int): String = i.toString
  def freeze(): Unit = {}
}


// For variables that hold a single discrete value

/** A value in a DiscreteDomain. */
trait DiscreteValue extends DiscretesValue with cc.factorie.la.SingletonBinaryVec {
  def domain: DiscreteDomain
  def index: Int  // TODO Consider removing this alias, and just always using intValue?
  final def intValue = index}

trait DiscreteDomain extends DiscretesDomain with IterableDomain[DiscreteValue] with ValueType[DiscreteValue] {
  thisDomain =>
  // Make method 'size' abstract again.
  // Note that we are reversing the order of the traditional size/length dependency
  // Note that this only works if DiscreteDomain is a class, not a trait.
  def size: Int
  def dimensionDomain = this
  /** If true, do not allow this domain to change. */
  protected var _frozen = false
  override def freeze(): Unit = _frozen = true
  /** Can new category values be added to this Domain? */
  def frozen = _frozen

  def allocSize = size // TODO Remove this?
  var maxRequestedInt: Int = 0

  /** Maps from integer index to the DiscreteValue objects */
  private val __elements = new scala.collection.mutable.ArrayBuffer[ValueType]
  def _elements = __elements // Define this way so that _elements can be overridden

  def values: IndexedSeq[ValueType] = __elements
  // Access sort of like a collection
  //def values: scala.collection.Seq[ValueType] = _elements
  def length = size
  def apply(index:Int): ValueType  = getValue(index)
  def unapply(value:ValueType): Option[Int] = if (value.domain == this) Some(value.index) else None
  def iterator = _elements.iterator

  // TODO Make this 'protected' so that only the 'getValue' method should construct these objects?
  class DiscreteValue(val index:Int) extends cc.factorie.DiscreteValue {
    //type DomainType = cc.factorie.DiscreteDomain
    final def singleIndex = index // needed for SingletonBainaryVec
    final def length = thisDomain.size // needed for SingletonBinaryVec
    def domain = thisDomain
    override def toString = index.toString
    override def equals(other:Any): Boolean = 
      other match { case other:DiscreteValue => this.index == other.index; case _ => false }
  }

  // TODO Consider renaming this method to something without the 'get'.  Perhaps valueAtIndex()
  def getValue(index:Int): ValueType = {
    if (index > maxRequestedInt) maxRequestedInt = index
    if (index >= size) throw new IllegalArgumentException("DiscreteDomain.getValue: index "+index+" larger than size "+size)
    if (index >= _elements.size) for (i <- _elements.size to index) _elements += new DiscreteValue(i) //.asInstanceOf[Value] // Here new a DiscreteValue gets created
    _elements(index)
  }

  // Serialization
  override def save(dirname:String): Unit = {
    val f = new File(dirname+"/"+filename)
    val s = new PrintWriter(new FileWriter(f))
    s.println(size)
    s.close
  }
  override def load(dirname:String): Unit = {
    val f = new File(dirname+"/"+filename)
    val s = new BufferedReader(new FileReader(f))
    val line = s.readLine
    val readSize = Integer.parseInt(line)
    require(size == readSize)
  }
}


