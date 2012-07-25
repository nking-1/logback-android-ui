package edu.vu.isis.logger.lib;

/*
 * Copyright 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Objects of this class are tree nodes. A tree node contains a list of leaves
 * which may be empty. A tree node has a payload, which is a reference to an
 * object. All nodes in a tree share the same dictionary (locate). The parent of
 * a tree node is the tree which contains this node as a leaf.
 * 
 * @param <T>
 *            Object's type in the tree.
 */
public class Tree<T> {

	final private T payload;
	final private ArrayList<Tree<T>> leafs;
	final private Tree<T> parent;
	final private HashMap<T, Tree<T>> locate;

	private Tree(T payload, HashMap<T, Tree<T>> locate, Tree<T> parent) {
		this.payload = payload;
		this.locate = locate;
		this.parent = parent;
		this.leafs = new ArrayList<Tree<T>>();
		locate.put(payload, this);
	}

	/**
	 * Construct the root tree node.
	 * 
	 * @param payload
	 */
	public static <T> Tree<T> newInstance(T payload) {
		return new Tree<T>(payload, new HashMap<T, Tree<T>>(), null);
	}

	/**
	 * Adds a leaf node for a particular payload to a node having the specified
	 * parent payload.
	 * 
	 * @param root
	 * @param leaf
	 */
	public void addLeaf(T root, T leaf) {
		if (locate.containsKey(root)) {
			locate.get(root).addLeaf(leaf);
		} else {
			throw new IllegalArgumentException("no parent with that name "
					+ root.toString());
		}
	}

	/**
	 * Add a leaf node with the specified payload to the current node.
	 * 
	 * @param leaf
	 * @return
	 */
	public Tree<T> addLeaf(T leaf) {
		Tree<T> t = new Tree<T>(leaf, this.locate, this);
		leafs.add(t);
		locate.put(leaf, t);
		return t;
	}

	public boolean contains(T element) {
		return locate.containsKey(element);
	}

	public T getPayload() {
		return payload;
	}

	public Tree<T> getTree(T element) {
		return locate.get(element);
	}

	public Tree<T> getParent() {
		return parent;
	}

	/**
	 * Gets all children of a particular tree node.
	 * 
	 * @param root
	 * @return
	 */
	public Collection<T> getSuccessors(T root) {
		final Collection<T> successors = new ArrayList<T>();
		Tree<T> tree = getTree(root);
		if (null != tree) {
			for (Tree<T> leaf : tree.leafs) {
				successors.add(leaf.payload);
			}
		}
		return successors;
	}

	public Collection<Tree<T>> getSubTrees() {
		return leafs;
	}

	public static <T> Collection<T> getSuccessors(T of, Collection<Tree<T>> in) {
		for (Tree<T> tree : in) {
			if (tree.locate.containsKey(of)) {
				return tree.getSuccessors(of);
			}
		}
		return new ArrayList<T>();
	}

	@Override
	public String toString() {
		return printTree(0);
	}

	private static final int indent = 2;

	private String printTree(int increment) {
		String s = "";
		String inc = "";
		for (int i = 0; i < increment; ++i) {
			inc = inc + " ";
		}
		s = inc + payload;
		for (Tree<T> child : leafs) {
			s += "\n" + child.printTree(increment + indent);
		}
		return s;
	}
}