/* Lock-Free Skip Tree - (c) 2010 University of Virginia */

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A lock-free concurrent skip tree, based on the algorithm of Spiegel
 * and Reynolds, "Lock-Free Multiway Search Trees" submitted for review
 * in ICPP 2010.
 * 
 * @author Michael Spiegel
 */

@SuppressWarnings("unchecked")
public class ConcurrentSkipTree<T extends Comparable<T>> {
    private static final int MAX_LEVEL = 32;
    private transient volatile HeadNode<T> root;

    private static final Random seedGenerator = new Random();
    private transient int randomSeed;
    
    /** CAS for volatile root reference */
    private static final AtomicReferenceFieldUpdater<ConcurrentSkipTree, HeadNode>
        rootUpdater = AtomicReferenceFieldUpdater.newUpdater(
                ConcurrentSkipTree.class, HeadNode.class, "root");
    
    public ConcurrentSkipTree() {
        randomSeed = seedGenerator.nextInt() | 0x0100;
        Object[] items = new Object[1];
        items[0] = PositiveInfinity.INSTANCE;
        Contents<T> contents = new Contents<T>(items, null, null);
        Node<T> node = new Node<T>(contents);
        rootUpdater.set(this, new HeadNode<T>(node, 0));
    }
    
    static final class PositiveInfinity<T extends Comparable<T>> implements Comparable<T> {
        private static final PositiveInfinity INSTANCE = new PositiveInfinity();
        private PositiveInfinity() {}

        @Override
        public int compareTo(T other) {
            return 1;
        }

        public String toString() {
            return "+&#8734;";
        }

    }
    
    static final class HeadNode<T extends Comparable<T>> {
        final Node<T> node;
        final int height;
        
        public HeadNode(Node<T> node, int height) {
            this.node = node;
            this.height = height;
        }
    }

    static final class Node<T extends Comparable<T>> {
        volatile Contents<T> contents;
        
        /** Updater for casContents */
        static final AtomicReferenceFieldUpdater<Node, Contents>
            contentsUpdater = AtomicReferenceFieldUpdater.newUpdater
            (Node.class, Contents.class, "contents");
                
        Node(Contents<T> contents) {
        	contentsUpdater.set(this, contents);
        }

        boolean casContents(Contents<T> expect, Contents<T> update) {
        	return(contentsUpdater.compareAndSet(this, expect, update));
        }
    }
    
    static final class Contents<T extends Comparable<T>> {
        final Object[] items;
        final Node<T>[] children;
        final Node<T> link;
        
        Contents(Object[] items, Node<T>[] children, Node<T> link) {
            this.items = items;
            this.children = children;
            this.link = link;
        }
    }
    
    static final class SearchResults<T extends Comparable<T>> {
        final Node<T> node;
        final Contents<T> contents;
        final int index;
        
        SearchResults(Node<T> node, Contents<T> contents, int index) {
            this.node = node;
            this.contents = contents;
            this.index = index;
        }
    }
    
    
    /**
     * Adds the specified element to this set if it is not already present.
     * More formally, adds the specified element <tt>x</tt> to this set if
     * the set contains no element <tt>e2</tt> such that <tt>x.equals(e2)</tt>.
     * If this set already contains the element, the call leaves the set
     * unchanged and returns <tt>false</tt>.
     *
     * @param e element to be added to this set
     * @return <tt>true</tt> if this set did not already contain the
     *         specified element
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(T x) {
        final int height = randomLevel();
        if (height == 0) {
            SearchResults results = traverseLeaf(x, false);
            return(insertLeafLevel(x, results));
        } else {
            SearchResults[] results = new SearchResults[height + 1];
            traverseNonLeaf(x, height, results);
            boolean success = insertOneLevel(x, results, null, 0);
            if(!success) return false;
            for(int i = 0; i < height; i++) {
                Node<T> right = splitOneLevel(x, results[i]);        	
                insertOneLevel(x, results, right, i + 1);
            }
            return true;
        }
    }

    /**
     * Returns <tt>true</tt> if this set contains the specified element.
     * More formally, returns <tt>true</tt> if and only if this set
     * contains an element <tt>x</tt> such that <tt>x.equals(e)</tt>.
     *
     * @param x object to be checked for containment in this set
     * @return <tt>true</tt> if this set contains the specified element
     * @throws NullPointerException if the specified element is null
     */    
    public boolean contains(T x) {
        Node<T> node = this.root.node;
        Contents<T> contents = node.contents;
        int index = search(contents.items, x);    
        while(contents.children != null) {
            if (-index - 1 == contents.items.length) {
                node = contents.link;
            } else if (index < 0) {
                node = contents.children[-index - 1];
            } else {
                node = contents.children[index];
            }
            contents = node.contents;
            index = search(contents.items, x);            
        }
        while(true) {
            if (-index - 1 == contents.items.length) {
                node = contents.link;
            } else if (index < 0) {
                return(false);
            } else {
                return(true);
            }
            contents = node.contents;
            index = search(contents.items, x);            
        }
    }

    /**
     * Removes the specified element from this set if it is present.
     * More formally, removes an element <tt>e</tt> such that
     * <tt>x.equals(e)</tt>, if this set contains such an element.
     * Returns <tt>true</tt> if this set contained the element (or
     * equivalently, if this set changed as a result of the call).
     * (This set will not contain the element once the call returns.)
     *
     * @param x object to be removed from this set, if present
     * @return <tt>true</tt> if this set contained the specified element
     * @throws NullPointerException if the specified element is null
     */
    public boolean remove(T x) {
        SearchResults results = traverseLeaf(x, true);
        while(true) {
            Node<T> node = results.node;
            Contents<T> contents = results.contents;
            int index = results.index;
            if (index < 0) {
                return(false);
            } else {
                Object[] newitems = removeSingleItem(contents.items, index);
                Contents<T> update = new Contents<T>(newitems, null, contents.link);
                if (node.casContents(contents, update)) {
                    return(true);
                } else {
                    results = moveForward(node, x);
                }                
            }
        }
    }
        
    /**
     * Traverses through the data structure to find the location
     * of the key in the bottom level (zero height) of the tree.
     * @param key   	the element to be searched for
     * @param cleanup	whether to perform node compaction
     * @return      	the {@link SearchResults} results
     */
    private SearchResults<T> traverseLeaf(T key, boolean cleanup) {
        Node<T> node = root.node;
        Contents<T> contents = node.contents;
        int index = search(contents.items, key); 
        T leftBarrier = null;
        while(contents.children != null) {
            if (-index - 1 == contents.items.length) {
                if (contents.items.length > 0) {
                    leftBarrier = (T) contents.items[contents.items.length - 1];
                }
                node = cleanLink(node, contents).link;
            } else {
                assert(contents.items.length > 0);
                if (index < 0) index = -index - 1;
                if (cleanup) cleanNode(key, node, contents, index, leftBarrier);
                node = contents.children[index];
                leftBarrier = null;                
            }
            contents = node.contents;
            index = search(contents.items, key);            
        }
        while(true) {
            if (index > -contents.items.length - 1) {
                return new SearchResults<T>(node, contents, index);
            } else {
                node = cleanLink(node, contents).link;
            }
            contents = node.contents;
            index = search(contents.items, key);
        }
    }    
    
    /**
     * Traverses through the data structure to find the locations
     * of the key in the tree. Store the locations of the key in the
     * storeResults array for heights {target, target - 1, ..., 0}.
     * 
     * @param key           the element to be searched for
     * @param target        the max height for populating the storeResults array
     * @param storeResults  the array of {@link SearchResults} results
     */    
    private void traverseNonLeaf(T key, int target, SearchResults[] storeResults) {
        HeadNode<T> root = this.root;
        if (root.height < target) {
            root = increaseRootHeight(target);
        }
        int height = root.height;
        Node<T> node = root.node;
        while(true) {
            Contents<T> contents = node.contents;
            int index = search(contents.items, key);
            if (-index - 1 == contents.items.length) {
                node = contents.link;
            } else if (height == 0) {
                storeResults[0] = new SearchResults<T>(node, contents, index);
                return;
            } else {
                SearchResults<T> results = new SearchResults<T>(node, contents, index);
                results = goodSamaritanCleanNeighbor(key, results);
                if (height <= target) {
                    storeResults[height] = results;
                }
                if (index < 0) index = -index - 1;
                node = contents.children[index];
                height = height - 1;    
            }
        }
    }    
    
    private static <T extends Comparable<T>> Contents<T> cleanLink(Node<T> node,
            Contents<T> contents) {        
        while(true) {
            Node<T> newLink = pushRight(contents.link, null);
            if (newLink == contents.link) return (contents);
            Contents<T> update = new Contents<T>(contents.items, contents.children, newLink);
            if (node.casContents(contents, update)) return(update);
            contents = node.contents;
        }
    }
    
    private static <T extends Comparable<T>> void cleanNode(T key,
            Node<T> node, Contents<T> contents, int index, T leftBarrier) {
        while(true) {
        	int length = contents.items.length;
            if (length == 0) {
                return;
            } else if (length == 1) {
                if(cleanNode1(node, contents, leftBarrier)) return;
            } else if (length == 2) {
                if(cleanNode2(node, contents, leftBarrier)) return;
            } else {
                if(cleanNodeN(node, contents, index, leftBarrier)) return;
            }
            contents = node.contents;
            index = search(contents.items, key);
            if (-index - 1 == contents.items.length) return;
            else if (index < 0) index = -index - 1;
        }
    }    
        
    private static <T extends Comparable<T>> boolean cleanNode1(Node<T> node,
    		Contents<T> contents, T leftBarrier) {
    	boolean success = attemptSlideKey(node, contents);
    	if (success) return(true);
    	Comparable<T> item = (Comparable<T>) contents.items[0];
    	if (leftBarrier != null && item.compareTo(leftBarrier) <= 0) {
    		leftBarrier = null;
    	}
        Node<T> childNode = contents.children[0];
        Node<T> adjustedChild = pushRight(childNode, leftBarrier);    	
        if (adjustedChild == childNode) return true;        
        return shiftChild(node, contents, 0, adjustedChild);
    }
    
    private static <T extends Comparable<T>> boolean cleanNode2(Node<T> node,
    		Contents<T> contents, T leftBarrier) {
    	boolean success = attemptSlideKey(node, contents);
    	if (success) return(true);
    	Comparable<T> item = (Comparable<T>) contents.items[0];    	
    	if (leftBarrier != null && item.compareTo(leftBarrier) <= 0) {
    		leftBarrier = null;
    	}
        Node<T> childNode1 = contents.children[0];
        Node<T> adjustedChild1 = pushRight(childNode1, leftBarrier);    	
        leftBarrier = (T) contents.items[0];
        Node<T> childNode2 = contents.children[1];
        Node<T> adjustedChild2 = pushRight(childNode2, leftBarrier);
        if ((adjustedChild1 == childNode1) && (adjustedChild2 == childNode2)) return true;  
        return shiftChildren(node, contents, adjustedChild1, adjustedChild2);
    }

    
    private static <T extends Comparable<T>> boolean cleanNodeN(Node<T> node,
            Contents<T> contents, int index, T leftBarrier) {
    	Comparable<T> item0 = (Comparable<T>) contents.items[0];    	    	
        if (index > 0) {
            leftBarrier = (T) contents.items[index - 1];
        } else if (leftBarrier != null && item0.compareTo(leftBarrier) <= 0) {
       		leftBarrier = null;
       	}
        Node<T> childNode = contents.children[index];
        Node<T> adjustedChild = pushRight(childNode, leftBarrier);
        if (index == 0 || index == contents.children.length - 1) {
            if (adjustedChild == childNode) return true;            
            return shiftChild(node, contents, index, adjustedChild);
        }
        Node<T> adjustedNeighbor = pushRight(contents.children[index + 1], 
        		(T) contents.items[index]);
        if (adjustedNeighbor == adjustedChild) {
            return dropChild(node, contents, index, adjustedChild);
        } else if (adjustedChild != childNode) {
            return shiftChild(node, contents, index, adjustedChild);            
        } else {
            return true;
        }
    }
    
    private static<T extends Comparable<T>> boolean attemptSlideKey(
    		Node<T> node, Contents<T> contents) {
        if (contents.link == null) return(false);
    	int length = contents.items.length;
    	T item = (T) contents.items[length - 1];
    	Node<T> child = contents.children[length - 1];
    	Node<T> sibling = pushRight(contents.link, null);
    	Contents<T> siblingContents = sibling.contents;
    	Node<T> nephew = null;
    	if (siblingContents.children.length == 0) {
    	    return(false);
    	} else {
    		nephew = siblingContents.children[0];
    	}
    	if (((Comparable<T>) siblingContents.items[0]).compareTo(item) > 0) {
    		nephew = pushRight(nephew, item);
    	} else {
    		nephew = pushRight(nephew, null);
    	}
    	if (nephew != child) return(false);
    	boolean success = slideToNeighbor(sibling, siblingContents, item, child);
    	if(success) deleteSlidedKey(node, contents, item);
    	return(true);
    }

    private static<T extends Comparable<T>> boolean slideToNeighbor(
    		Node<T> sibling, Contents<T> sContents, T item, Node<T> child) {
    	int index = search(sContents.items, item);
    	if (index >= 0) {
    		return(true);
    	} else if (index < -1) {
    		return(false);
    	}
    	Object[] items = generateNewItems(item, sContents.items, 0);
    	Node<T>[] children = generateNewChildren(child, sContents.children, 0);
    	Contents<T> update = new Contents<T>(items, children, sContents.link);
    	if (sibling.casContents(sContents, update)) {
    		return(true);
    	} else {
    		return(false);
    	}
    }

    private static<T extends Comparable<T>> Contents<T> deleteSlidedKey(
    		Node<T> node, Contents<T> contents, T item) {
    	int index = search(contents.items, item);
    	if (index < 0) return(contents);
    	Object[] items = removeSingleItem(contents.items, index);
    	Node<T>[] children = removeSingleChild(contents.children, index);
    	Contents<T> update = new Contents<T>(items, children, contents.link);
    	if (node.casContents(contents, update)) {
    	    return(update);
    	} else {
    	    return(contents);
    	}
    }    

    
    private static <T extends Comparable<T>> boolean dropChild(Node<T> node,
            Contents<T> contents, int index, Node<T> adjustedChild) {
        int length = contents.items.length;
        Object[] newItems = new Object[length - 1];
        Node<T>[] newChildren = new Node[length - 1];
        for(int i = 0; i < index; i++) {
            newItems[i] = contents.items[i];
            newChildren[i] = contents.children[i];
        }
        newChildren[index] = adjustedChild;
        for(int i = index + 1; i < length; i++) {
            newItems[i - 1] = contents.items[i];
        }
        for(int i = index + 2; i < length; i++) {
            newChildren[i - 1] = contents.children[i];
        }        
        Contents<T> update = new Contents<T>(newItems, 
                newChildren, contents.link);
        return node.casContents(contents, update);
    }

    private static <T extends Comparable<T>> boolean shiftChild(Node<T> node, 
            Contents<T> contents, int index, Node<T> adjustedChild) {
        Node<T>[] newChildren = Arrays.copyOf(contents.children, 
                contents.children.length);
        newChildren[index] = adjustedChild;
        Contents<T> update = new Contents<T>(contents.items, 
                newChildren, contents.link);
        return node.casContents(contents, update);
    }
    
    private static <T extends Comparable<T>> boolean shiftChildren(Node<T> node, 
            Contents<T> contents, Node<T> child1, Node<T> child2) {
        Node<T>[] newChildren = new Node[2];
        newChildren[0] = child1;
        newChildren[1] = child2;        
        Contents<T> update = new Contents<T>(contents.items, 
                newChildren, contents.link);
        return node.casContents(contents, update);
    }
    
    private static <T extends Comparable<T>> Node<T> pushRight(Node<T> node, T leftBarrier) {
        while(true) {
            Contents<T> contents = node.contents;
            int length = contents.items.length;
            if (length == 0) {
                node = contents.link;                
            } else if (leftBarrier == null || 
                    ((Comparable<T>) contents.items[length - 1]).compareTo(leftBarrier) > 0) {
                return node;
            } else {
                node = contents.link;
            }
        }
    }

	private static <T extends Comparable<T>> SearchResults goodSamaritanCleanNeighbor(
			T key, SearchResults<T> results) {
	    Node<T> node = results.node;
	    Contents<T> contents = results.contents;
        if (contents.link == null) return(results);
        int length = contents.items.length;
        T item = (T) contents.items[length - 1];
        Node<T> child = contents.children[length - 1];
        Node<T> sibling = pushRight(contents.link, null);
        Contents<T> siblingContents = sibling.contents;
        Node<T> nephew = null, adjustedNephew = null;
        if (siblingContents.children.length == 0) {
            contents = cleanLink(node, node.contents);
            int index = search(contents.items, key);
            return new SearchResults(node, contents, index);
        } else {
            nephew = siblingContents.children[0];
        }        
        if (((Comparable<T>) siblingContents.items[0]).compareTo(item) > 0) {
            adjustedNephew = pushRight(nephew, item);
        } else {
            adjustedNephew = pushRight(nephew, null);
        }
        if (nephew != child) {
            if (adjustedNephew != nephew) shiftChild(sibling, siblingContents, 0, adjustedNephew);
        } else {
            boolean success = slideToNeighbor(sibling, siblingContents, item, child);
            if(success) {
                contents = deleteSlidedKey(node, contents, item);
                int index = search(contents.items, key);
                return new SearchResults(node, contents, index);
            }
        }
        return(results);
	}    

	/**
	 * Moves forward along the same level as the input node and
	 * stop after locating the node that either contains or would
	 * contain the key.
	 *  
	 * @param node  the starting node for the search
	 * @param key   the target key
	 * @return      the {@link SearchResults} for the target key
	 */
	private static<T extends Comparable<T>> SearchResults<T> moveForward(
			Node<T> node, T key) {
	    while(true) {
	        Contents<T> contents = node.contents;
	        int index = search(contents.items, key);
	        if (index > -contents.items.length - 1) {
                return new SearchResults<T>(node, contents, index);	            
	        } else {
	            node = contents.link;
	        }
	    }
	}

	/**
	 * Splits a linked list level using element x as a pivot element.
	 * The {@link SearchResults} provide a hint as to where x is located.
	 * A split is performed if-and-only-if (a) the node contains the target
	 * key, (b) the node contains at least two elements, and (c) the target
	 * key is not the last element of the node. After the split, the node is
	 * replaced with the new "left" node, which contains x and elements less
	 * than x.  The function returns the "right" node which is created during
	 * the split operation.
	 * 
	 * @param x        the element to use as a pivot
	 * @param results  a hint to locate element x
	 * @return         the new "right" node
	 */
	private Node<T> splitOneLevel(T x, SearchResults<T> results) {
        while(true) {
            Node<T> node = results.node;
            Contents<T> contents = results.contents;
            int index = results.index;
            int length = contents.items.length;
            if (index < 0) {
                return(null);
            } else if (length < 2 || index == (length - 1)) {
                return(null);
            }
            Object[] leftItems = generateLeftItems(contents.items, index);
            Object[] rightItems = generateRightItems(contents.items, index);
            Node<T>[] leftChildren = generateLeftChildren(contents.children, index);
            Node<T>[] rightChildren = generateRightChildren(contents.children, index);
            Node<T> right = new Node<T>(new Contents<T>(rightItems, rightChildren, contents.link));
            Contents<T> left = new Contents<T>(leftItems, leftChildren, right);
            if (node.casContents(contents, left)) {
                return(right);
            } else {
                results = moveForward(node, x);
            }
        }
    }
	
    private boolean insertLeafLevel(T x, SearchResults results) {
        while(true) {
            Node<T> node = results.node;
            Contents<T> contents = results.contents;
            int index = results.index;
            if (index >= 0) {
                return(false);
            } else {
                index = - index - 1;
                assert(index >= 0);
                assert(index < contents.items.length);
                Object[] newitems = generateNewItems(x, contents.items, index);
                Contents<T> update = new Contents<T>(newitems, null, contents.link);
                if (node.casContents(contents, update)) {
                    return(true);
                } else {
                    results = moveForward(node, x);
                }
            }
        }        
    }
	
    
    private boolean insertOneLevel(T x, SearchResults[] resultsStore, 
            Node<T> child, int target) {
        if (child == null && target > 0) {
            return false;
        }
        SearchResults results = resultsStore[target];
        while(true) {
            Node<T> node = results.node;
            Contents<T> contents = results.contents;
            int index = results.index;
            if (index >= 0) {
                return false;
            } else if (index > -contents.items.length - 1) {
                index = - index - 1;
                assert(index >= 0 && index < contents.items.length);
                Object[] newitems = generateNewItems(x, contents.items, index);
                Node<T>[] newchildren = generateNewChildren(child, contents.children, index + 1);
                Contents<T> update = new Contents<T>(newitems, newchildren, 
                        contents.link);
                if (node.casContents(contents, update)) {                        
                    SearchResults newResults = new SearchResults<T>(node, update, index);
                    resultsStore[target] = newResults;
                    return(true);
                } else {
                    results = moveForward(node, x);
                }
            } else {
                assert(index == - contents.items.length - 1);                
                results = moveForward(node, x);
            }
        }
    }
    
    private static Object[] removeSingleItem(Object[] items, int index) {
        int length = items.length;
        Object[] newitems = new Object[length - 1];
        for(int i = 0; i < index; i++) {
            newitems[i] = items[i];
        }
        for(int i = index + 1; i < length; i++) {
            newitems[i - 1] = items[i];
        }
        return(newitems);
    }
    
    private static <T extends Comparable<T>> Node<T>[] removeSingleChild(
    		Node<T>[] children, int index) {
        int length = children.length;
        Node<T>[] newchildren = new Node[length - 1];
        for(int i = 0; i < index; i++) {
        	newchildren[i] = children[i];
        }
        for(int i = index + 1; i < length; i++) {
        	newchildren[i - 1] = children[i];
        }
        return(newchildren);
    }
    
    private static <T extends Comparable<T>> Object[] generateNewItems(
    		T x, Object[] items, int index) {
        int length = items.length;
        Object[] newitems = new Object[length + 1];
        for(int i = 0; i < index; i++) {
            newitems[i] = items[i];
        }
        newitems[index] = x;
        for(int i = index; i < length; i++) {
            newitems[i + 1] = items[i];
        }
        return(newitems);
    }    
    
    private static <T extends Comparable<T>> Node<T>[] generateNewChildren(
    		Node<T> child, Node<T>[] children, 
            int index) {
        if (child == null) return(null);
        int length = children.length;
        Node<T>[] newchildren = new Node[length + 1];
        for(int i = 0; i < index; i++) {
            newchildren[i] = children[i];
        }
        newchildren[index] = child;
        for(int i = index; i < length; i++) {
            newchildren[i + 1] = children[i];
        }
        return(newchildren);
    }
        
    private static Object[] generateLeftItems(Object[] items, int index) {
        Object[] newitems = new Object[index + 1];
        for(int i = 0; i <= index; i++) {
            newitems[i] = items[i];
        }
        return(newitems);
    }

    private static Object[] generateRightItems(Object[] items, int index) {
        int length = items.length;
        Object[] newitems = new Object[length - index - 1];
        for(int i = 0, j = index + 1; j < length; i++, j++) {
            newitems[i] = items[j];
        }
        return(newitems);
    }
    
    private static <T extends Comparable<T>> Node<T>[] generateLeftChildren(
    		Node<T>[] children, int index) {
        if (children == null) return(null);
        Node<T>[] newchildren = new Node[index + 1];
        for(int i = 0; i <= index; i++) {
            newchildren[i] = children[i];
        }
        return(newchildren);
    }

    private static <T extends Comparable<T>> Node<T>[] generateRightChildren(
    		Node<T>[] children, int index) {
        if (children == null) return(null);
        int length = children.length;
        Node<T>[] newchildren  = new Node[length - index - 1];
        for(int i = 0, j = index + 1; j < length; i++, j++) {
            newchildren[i] = children[j];
        }
        return(newchildren);
    }
        
    private HeadNode<T> increaseRootHeight(int target) {
        HeadNode<T> root = this.root;
        int height = root.height;
        while(height < target) {
            Object[] items = new Object[1];
            Node<T>[] children = new Node[1];
            items[0] = PositiveInfinity.INSTANCE;            
            children[0] = root.node;
            Contents<T> contents = new Contents<T>(items, children, null);
            Node<T> newNode = new Node<T>(contents);
            HeadNode<T> update = new HeadNode<T>(newNode, height + 1);
            rootUpdater.compareAndSet(this, root, update);
            root = this.root;
            height = root.height;
        }
        return(root);
    }
    
    private static<T extends Comparable<T>> int search(Object[] a, T key) {
        int low = 0;
        int high = a.length - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Comparable<T> midVal = (Comparable<T>) a[mid];
            int cmp = midVal.compareTo(key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        
        return -(low + 1);  // key not found.
    }
        
    /**
     * Returns a random level for inserting a new node.
     * Hardwired to k=1, q=0.03125, max is (MAX_LEVEL - 1)
     * 
     * Based on the ConcurrentSkipListMap implementation 
     * from the Java JDK java.util.concurrent package, 
     * written by Doug Lea with assistance from members of
     * JCP JSR-166 Expert Group and released to the public
     * domain, as explained at http://creativecommons.org/licenses/publicdomain
     */
    int randomLevel() {
        int x = randomSeed;
        x ^= x << 13;
        x ^= x >>> 17;
        randomSeed = x ^= x << 5;
        int level = 1;
        while ( ((x & 0x1F) == 0) && (level < MAX_LEVEL) ) {
            if ((level % 6) == 0) {
                x = randomSeed;
                x ^= x << 13;
                x ^= x >>> 17;
                randomSeed = x ^= x << 5;
            } else {
                x >>>= 5;
            }
            level++;            
        }
        return (level - 1);
    }

}