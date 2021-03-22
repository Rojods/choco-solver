/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2021, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.graph.degree;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.DirectedGraphVar;
import org.chocosolver.solver.variables.GraphVar;
import org.chocosolver.solver.variables.IncidentSet;
import org.chocosolver.solver.variables.UndirectedGraphVar;
import org.chocosolver.solver.variables.events.GraphEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.graphs.Orientation;
import org.chocosolver.util.objects.setDataStructures.ISet;

/**
 * Propagator that ensures that a node has at most N successors/predecessors/neighbors
 *
 * @author Jean-Guillaume Fages
 */
public class PropNodeDegreeAtMostCoarse extends Propagator<GraphVar> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private GraphVar g;
    private int[] degrees;
    private IncidentSet target;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public PropNodeDegreeAtMostCoarse(DirectedGraphVar graph, Orientation setType, int degree) {
        this(graph, setType, buildArray(degree, graph.getNbMaxNodes()));
    }

    public PropNodeDegreeAtMostCoarse(DirectedGraphVar graph, Orientation setType, int[] degrees) {
        super(new DirectedGraphVar[]{graph}, PropagatorPriority.BINARY, false);
        g = graph;
        this.degrees = degrees;
        switch (setType) {
            case SUCCESSORS:
                target = new IncidentSet.SuccessorsSet();
                break;
            case PREDECESSORS:
                target = new IncidentSet.PredecessorsSet();
                break;
            default:
                throw new UnsupportedOperationException("wrong parameter: use either PREDECESSORS or SUCCESSORS");
        }
    }

    public PropNodeDegreeAtMostCoarse(UndirectedGraphVar graph, int degree) {
        this(graph, buildArray(degree, graph.getNbMaxNodes()));
    }

    public PropNodeDegreeAtMostCoarse(final UndirectedGraphVar graph, int[] degrees) {
        super(new UndirectedGraphVar[]{graph}, PropagatorPriority.BINARY, false);
        target = new IncidentSet.SuccessorsSet();
        g = graph;
        this.degrees = degrees;
    }

    private static int[] buildArray(int degree, int n) {
        int[] degrees = new int[n];
        for (int i = 0; i < n; i++) {
            degrees[i] = degree;
        }
        return degrees;
    }

    //***********************************************************************************
    // PROPAGATIONS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        ISet act = g.getPotentialNodes();
        for (int node : act) {
            checkAtMost(node);
        }
    }

    //***********************************************************************************
    // INFO
    //***********************************************************************************

    @Override
    public int getPropagationConditions(int vIdx) {
        return GraphEventType.ADD_EDGE.getMask();
    }

    @Override
    public ESat isEntailed() {
        ISet act = g.getMandatoryNodes();
        for (int i : act) {
            if (target.getPotentialSet(g, i).size() > degrees[i]) {
                return ESat.FALSE;
            }
        }
        if (!g.isInstantiated()) {
            return ESat.UNDEFINED;
        }
        return ESat.TRUE;
    }

    //***********************************************************************************
    // PROCEDURES
    //***********************************************************************************

    /**
     * When a node has more than N successors/predecessors/neighbors then it must be removed,
     * (which results in a failure)
     * If it has N successors/predecessors/neighbors in the kernel then other incident edges
     * should be removed
     */
    private void checkAtMost(int i) throws ContradictionException {
        ISet ker = target.getMandatorySet(g, i);
        ISet env = target.getPotentialSet(g, i);
        int size = ker.size();
        if (size > degrees[i]) {
            g.removeNode(i, this);
        } else if (size == degrees[i] && env.size() > size) {
            for (int other : env) {
                if (!ker.contains(other)) {
                    target.remove(g, i, other, this);
                }
            }
        }
    }
}