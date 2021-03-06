package org.batfish.question.interfaceproperties;

import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import javax.annotation.Nonnull;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.hsrp.HsrpGroup;
import org.batfish.datamodel.questions.InterfacePropertySpecifier;
import org.batfish.datamodel.questions.InterfacesSpecifier;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableMetadata;
import org.junit.Before;
import org.junit.Test;

public class InterfacePropertiesAnswererTest {

  private static final String HOSTNAME = "node1";
  private static final String INTERFACE_NAME = "Ethernet0";

  private Configuration _c;
  private Configuration.Builder _cb;
  private Interface.Builder _ib;
  private NetworkFactory _nf;
  private Vrf _v;
  private Vrf.Builder _vb;

  private Object getActualValue(String property, Schema schema) {
    return getRows(property).iterator().next().get(property, schema);
  }

  @Test
  public void getProperties() {
    Configuration conf1 = new Configuration("node1", ConfigurationFormat.CISCO_IOS);

    Interface iface1 = new Interface("iface1", conf1);
    iface1.setDescription("desc desc desc");
    iface1.setActive(false);

    Interface iface2 = new Interface("iface2", conf1);
    iface2.setDescription("blah blah blah");

    conf1.getInterfaces().putAll(ImmutableMap.of("iface1", iface1, "iface2", iface2));

    String property1 = "description";
    String property2 = "active";
    InterfacePropertySpecifier propertySpecifier =
        new InterfacePropertySpecifier(property1 + "|" + property2);

    Multiset<Row> propertyRows =
        InterfacePropertiesAnswerer.getProperties(
            propertySpecifier,
            ImmutableMap.of("node1", conf1),
            ImmutableSet.of("node1"),
            new InterfacesSpecifier("iface1"),
            new TableMetadata(
                    InterfacePropertiesAnswerer.createColumnMetadata(propertySpecifier),
                    (String) null)
                .toColumnMap());

    // we should have exactly one row1 with two properties; iface2 should have been filtered out
    Row expectedRow =
        Row.builder()
            .put(
                InterfacePropertiesAnswerer.COL_INTERFACE, new NodeInterfacePair("node1", "iface1"))
            .put(property2, false)
            .put(property1, "desc desc desc")
            .build();

    assertThat(propertyRows, equalTo(ImmutableMultiset.of(expectedRow)));
  }

  @Test
  public void getPropertiesExcludeShutInterfaces() {
    Configuration conf = new Configuration("node", ConfigurationFormat.CISCO_IOS);
    Interface active = Interface.builder().setName("active").setOwner(conf).setActive(true).build();
    Interface shut = Interface.builder().setName("shut").setOwner(conf).setActive(false).build();
    conf.getInterfaces().putAll(ImmutableMap.of("active", active, "shut", shut));

    String property = "description";
    InterfacePropertySpecifier propertySpecifier = new InterfacePropertySpecifier(property);

    Multiset<Row> propertyRows =
        InterfacePropertiesAnswerer.getProperties(
            propertySpecifier,
            ImmutableMap.of("node", conf),
            ImmutableSet.of("node"),
            InterfacesSpecifier.ALL,
            true,
            new TableMetadata(
                    InterfacePropertiesAnswerer.createColumnMetadata(propertySpecifier),
                    (String) null)
                .toColumnMap());

    // we should have exactly one row; iface1 should have been filtered out
    assertThat(propertyRows, hasSize(1));
    assertThat(
        propertyRows.iterator().next(),
        hasColumn(
            InterfacePropertiesAnswerer.COL_INTERFACE,
            equalTo(new NodeInterfacePair("node", "active")),
            Schema.INTERFACE));
  }

  private @Nonnull Multiset<Row> getRows(String property) {
    InterfacePropertySpecifier propertySpecifier = new InterfacePropertySpecifier(property);
    return InterfacePropertiesAnswerer.getProperties(
        propertySpecifier,
        ImmutableMap.of(HOSTNAME, _c),
        ImmutableSet.of(HOSTNAME),
        new InterfacesSpecifier(INTERFACE_NAME),
        new TableMetadata(
                InterfacePropertiesAnswerer.createColumnMetadata(propertySpecifier), (String) null)
            .toColumnMap());
  }

  @Before
  public void setup() {
    _nf = new NetworkFactory();
    _cb =
        _nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname(HOSTNAME);
    _c = _cb.build();
    _vb = _nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME).setOwner(_c);
    _v = _vb.build();
    _ib = _nf.interfaceBuilder().setVrf(_v).setOwner(_c).setName(INTERFACE_NAME);
  }

  @Test
  public void testHsrpGroups() {
    _ib.setHsrpGroups(ImmutableMap.of(1, HsrpGroup.builder().setGroupNumber(1).build())).build();
    String property = "hsrp-groups";

    assertThat(getActualValue(property, Schema.set(Schema.STRING)), equalTo(ImmutableSet.of("1")));
  }

  @Test
  public void testHsrpGroupsEmpty() {
    _ib.build();
    String property = "hsrp-groups";

    assertThat(getActualValue(property, Schema.set(Schema.STRING)), equalTo(ImmutableSet.of()));
  }

  @Test
  public void testHsrpVersion() {
    _ib.setHsrpVersion("2").build();
    String property = "hsrp-version";

    assertThat(getActualValue(property, Schema.STRING), equalTo("2"));
  }

  @Test
  public void testHsrpVersionUnset() {
    _ib.build();
    String property = "hsrp-version";

    assertThat(getActualValue(property, Schema.STRING), nullValue());
  }
}
