package org.batfish.symbolic.abstraction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Prefix;
import org.batfish.symbolic.Graph;
import org.batfish.symbolic.bdd.BDDNetwork;
import org.batfish.symbolic.utils.Tuple;

public class NetworkSlice {

  private HeaderSpace _headerSpace;

  private Abstraction _abstraction;

  private List<Prefix> _destPrefixes;

  public NetworkSlice(
      HeaderSpace headerSpace, @Nullable Abstraction abstraction, List<Prefix> destPrefixes) {
    this._headerSpace = headerSpace;
    this._abstraction = abstraction;
    this._destPrefixes = destPrefixes;
  }

  public static ArrayList<Supplier<NetworkSlice>> allSlices(DestinationClasses dcs, int fails) {
    BDDNetwork network = BDDNetwork.create(dcs.getGraph());
    ArrayList<Supplier<NetworkSlice>> classes = new ArrayList<>();
    for (Entry<Set<String>, Tuple<HeaderSpace, List<Prefix>>> entry :
        dcs.getHeaderspaceMap().entrySet()) {
      Set<String> devices = entry.getKey();
      HeaderSpace headerspace = entry.getValue().getFirst();
      List<Prefix> prefixes = entry.getValue().getSecond();
      Supplier<NetworkSlice> sup =
          () -> AbstractionBuilder.createGraph(dcs, network, devices, headerspace, prefixes, fails);
      classes.add(sup);
    }
    return classes;
  }

  public HeaderSpace getHeaderSpace() {
    return _headerSpace;
  }

  public Abstraction getAbstraction() {
    return _abstraction;
  }

  public List<Prefix> getDestPrefixes() {
    return _destPrefixes;
  }

  public Graph getGraph() {
    return _abstraction.getGraph();
  }

  /*
   * Apply mapping from concrete to abstract nodes
   */
  public Set<String> mapConcreteToAbstract(Collection<String> concreteNodes) {
    Abstraction abstraction = getAbstraction();
    return abstraction.mapConcreteToAbstract(concreteNodes);
  }
}