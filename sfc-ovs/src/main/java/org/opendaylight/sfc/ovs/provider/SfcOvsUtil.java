/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * SfcOvsUtil class contains various wrapper and utility methods
 * <p>
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-04-01
 */

package org.opendaylight.sfc.ovs.provider;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.sfc.ovs.api.SfcOvsDataStoreAPI;
import org.opendaylight.sfc.ovs.api.SfcSffToOvsMappingAPI;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridge;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridgeBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeNetdev;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdk;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SfcOvsUtil {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsUtil.class);
    private static final String HEX = "0x";
    private static final String OPENFLOW = "openflow:";
    private static final String OVSDB_BRIDGE_PREFIX = "/bridge/";
    public static final String OVSDB_OPTION_LOCAL_IP = "local_ip";
    public static final String OVSDB_OPTION_REMOTE_IP = "remote_ip";
    public static final String OVSDB_OPTION_DST_PORT = "dst_port";
    public static final String OVSDB_OPTION_NSP = "nsp";
    public static final String OVSDB_OPTION_NSI = "nsi";
    public static final String OVSDB_OPTION_IN_NSP = "in_nsp";
    public static final String OVSDB_OPTION_IN_NSI = "in_nsi";
    public static final String OVSDB_OPTION_OUT_NSP = "out_nsp";
    public static final String OVSDB_OPTION_OUT_NSI = "out_nsi";
    public static final String OVSDB_OPTION_NSHC1 = "nshc1";
    public static final String OVSDB_OPTION_NSHC2 = "nshc2";
    public static final String OVSDB_OPTION_NSHC3 = "nshc3";
    public static final String OVSDB_OPTION_NSHC4 = "nshc4";
    public static final String OVSDB_OPTION_KEY = "key";
    public static final String OVSDB_OPTION_EXTS = "exts";
    public static final String OVSDB_OPTION_GPE = "gpe";
    public static final String OVSDB_OPTION_VALUE_FLOW = "flow";
    public static final String DPL_NAME_DPDK = "Dpdk";
    public static final String DPL_NAME_DPDKVHOST = "Dpdkvhost";
    public static final String DPL_NAME_DPDKVHOSTUSER = "Dpdkvhostuser";
    public static final String DPL_NAME_INTERNAL = "Internal";
    public static final PortNumber NSH_VXLAN_TUNNEL_PORT = new PortNumber(6633);

    private static final Predicate<Options> FLOW_BASED_OPT = (option) ->
            Objects.equals(option.getOption(), OVSDB_OPTION_REMOTE_IP)
            && Objects.equals(option.getValue(), OVSDB_OPTION_VALUE_FLOW);
    private static final Predicate<Options> GPE_OPT = (option) ->
            Objects.equals(option.getOption(), OVSDB_OPTION_EXTS)
            && Objects.equals(option.getValue(), OVSDB_OPTION_GPE);
    private static final Predicate<Options> FLOW_BASED_OR_GPE_OPT = Stream.of(FLOW_BASED_OPT, GPE_OPT)
            .reduce(Predicate::or)
            .orElse(x -> false);

    private SfcOvsUtil() {
    }

    /**
     * Method builds OVSDB Topology InstanceIdentifier.
     *
     * <p>
     *
     * @return InstanceIdentifier&lt;Topology&gt;
     */
    public static InstanceIdentifier<Topology> buildOvsdbTopologyIID() {
        return InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));
    }

    /**
     * Method builds OVS NodeId which is based on: 1. OVS Node
     * InstanceIdentifier which manages the OVS Bridge 2. OVS Bridge name
     *
     * <p>
     * If the two aforementioned fields are missing, NullPointerException is
     * raised.
     *
     * <p>
     *
     * @param ovsdbBridge
     *            OvsdbBridgeAugmentation
     * @return NodeId
     */
    private static NodeId getOvsBridgeNodeId(OvsdbBridgeAugmentation ovsdbBridge) {
        Preconditions.checkNotNull(ovsdbBridge, "Cannot getOvsBridgeNodeId, OvsdbBridgeAugmentation is null.");

        Preconditions.checkNotNull(ovsdbBridge.getBridgeName(), "Cannot build getOvsBridgeNodeId, BridgeName is null.");
        Preconditions.checkNotNull(ovsdbBridge.getManagedBy(), "Cannot build getOvsBridgeNodeId, ManagedBy is null.");
        String bridgeName = ovsdbBridge.getBridgeName().getValue();
        InstanceIdentifier<Node> nodeIID = (InstanceIdentifier<Node>) ovsdbBridge.getManagedBy().getValue();

        KeyedInstanceIdentifier<?, ?> keyedInstanceIdentifier = (KeyedInstanceIdentifier<?, ?>) nodeIID
                .firstIdentifierOf(Node.class);
        Preconditions.checkNotNull(keyedInstanceIdentifier,
                "Cannot build getOvsBridgeNodeId, parent OVS Node is null.");

        NodeKey nodeKey = (NodeKey) keyedInstanceIdentifier.getKey();
        String nodeId = nodeKey.getNodeId().getValue();
        nodeId = nodeId.concat(OVSDB_BRIDGE_PREFIX + bridgeName);

        return new NodeId(nodeId);
    }

    /**
     * Given an OVS bridge Node, return the Managing OvsdbNode.
     *
     * @param ovsdbBridge
     *            OvsdbBridgeAugmentation
     * @return OvsdbNodeAugmentation - the managing node
     */
    public static OvsdbNodeAugmentation getManagerNodeByBridgeNode(OvsdbBridgeAugmentation ovsdbBridge) {
        if (ovsdbBridge.getManagedBy() == null) {
            LOG.warn("OVS bridge [{}] has a null ManagedBy entry", ovsdbBridge.getBridgeName().getValue());
            return null;
        }

        if (ovsdbBridge.getManagedBy().getValue() == null) {
            LOG.warn("OVS bridge [{}] has a null ManagedBy value", ovsdbBridge.getBridgeName().getValue());
            return null;
        }

        InstanceIdentifier<Node> nodeIID = (InstanceIdentifier<Node>) ovsdbBridge.getManagedBy().getValue();
        Node node = SfcDataStoreAPI.readTransactionAPI(nodeIID, LogicalDatastoreType.OPERATIONAL);

        if (node == null) {
            LOG.warn("OVS bridge [{}] ManagedBy node does not exist", ovsdbBridge.getBridgeName().getValue());
            return null;
        }

        return node.augmentation(OvsdbNodeAugmentation.class);
    }

    /**
     * Method builds OVS Node InstanceIdentifier which is based on OVS NodeId.
     *
     * <p>
     *
     * @param ovsdbBridge
     *            OvsdbBridgeAugmentation
     * @return InstanceIdentifier&lt;Node&gt;
     * @see SfcOvsUtil getOvsBridgeNodeId
     */
    public static InstanceIdentifier<Node> buildOvsdbNodeIID(OvsdbBridgeAugmentation ovsdbBridge) {
        return InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(getOvsBridgeNodeId(ovsdbBridge)));
    }

    /**
     * Method builds OVS Node InstanceIdentifier which is based on Service
     * Function Forwarder name. Method will return valid InstanceIdentifier only
     * if the given SFF name belongs to SFF instance mapped to OVS.
     *
     * <p>
     *
     * @param serviceFunctionForwarderName
     *            String
     * @return InstanceIdentifier&lt;Node&gt;
     */
    public static InstanceIdentifier<Node> buildOvsdbNodeIID(String serviceFunctionForwarderName) {
        return InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(serviceFunctionForwarderName)));
    }

    /**
     * Method builds OVS Node InstanceIdentifier which is based on NodeId.
     *
     * <p>
     *
     * @param nodeId
     *            NodeId
     * @return InstanceIdentifier&lt;Node&gt;
     */
    public static InstanceIdentifier<Node> buildOvsdbNodeIID(NodeId nodeId) {
        return InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(nodeId));
    }

    /**
     * Method builds OVS BridgeAugmentation InstanceIdentifier which is based on
     * OVS NodeId.
     *
     * <p>
     * @param ovsdbBridge
     *            OvsdbBridgeAugmentation
     * @return InstanceIdentifier&lt;OvsdbBridgeAugmentation&gt;
     * @see SfcOvsUtil getOvsBridgeNodeId
     */
    public static InstanceIdentifier<OvsdbBridgeAugmentation> buildOvsdbBridgeIID(OvsdbBridgeAugmentation ovsdbBridge) {
        return InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(getOvsBridgeNodeId(ovsdbBridge)))
                .augmentation(OvsdbBridgeAugmentation.class);
    }

    /**
     * Create a {@link InstanceIdentifier} {@link OvsdbBridgeAugmentation} based
     * on the Topology {@link NodeId}.
     *
     * @param nodeId
     *            A topology {@link NodeId}
     * @return InstanceIdentifier&lt;OvsdbBridgeAugmentation&gt;
     */
    public static InstanceIdentifier<OvsdbBridgeAugmentation> buildOvsdbBridgeIID(NodeId nodeId) {
        return InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(nodeId)).augmentation(OvsdbBridgeAugmentation.class);
    }

    /**
     * Method builds OVS BridgeAugmentation InstanceIdentifier which is based on
     * OVS Bridge name.
     *
     * <p>
     * @param serviceFunctionForwarderName
     *            serviceFunctionForwarderName String
     * @return InstanceIdentifier&lt;OvsdbBridgeAugmentation&gt;
     */
    public static InstanceIdentifier<OvsdbBridgeAugmentation> buildOvsdbBridgeIID(String serviceFunctionForwarderName) {
        return buildOvsdbNodeIID(serviceFunctionForwarderName)
                .augmentation(OvsdbBridgeAugmentation.class);
    }

    /**
     * Method builds OVS TerminationPointAugmentation InstanceIdentifier which
     * is based on: 1. OVS Node InstanceIdentifier which manages the OVS Bridge,
     * to which the OVS TerminationPoint is attached 2. OVS Termination Point
     * name.
     *
     * <p>
     * If the two aforementioned fields are missing, NullPointerException is
     * raised.
     *
     * <p>
     * @param ovsdbBridge
     *            OvsdbBridgeAugmentation
     * @param ovsdbTerminationPoint
     *            OvsdbTerminationPointAugmentation
     * @return InstanceIdentifier&lt;OvsdbTerminationPointAugmentation&gt;
     */
    public static InstanceIdentifier<OvsdbTerminationPointAugmentation> buildOvsdbTerminationPointAugmentationIID(
            OvsdbBridgeAugmentation ovsdbBridge, OvsdbTerminationPointAugmentation ovsdbTerminationPoint) {

        Preconditions.checkNotNull(ovsdbTerminationPoint,
                "Cannot build OvsdbTerminationPointAugmentation InstanceIdentifier,"
                + " OvsdbTerminationPointAugmentation is null.");
        Preconditions.checkNotNull(ovsdbTerminationPoint.getName(),
                "Cannot build OvsdbTerminationPointAugmentation InstanceIdentifier,"
                + " OvsdbTerminationPointAugmentation Name is null.");
        Preconditions.checkNotNull(ovsdbBridge,
                "Cannot build OvsdbTerminationPointAugmentation InstanceIdentifier, OvsdbBridgeAugmentation is null.");

        NodeId nodeId = getOvsBridgeNodeId(ovsdbBridge);
        String terminationPointId = ovsdbTerminationPoint.getName();

        return InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(nodeId))
                .child(TerminationPoint.class, new TerminationPointKey(new TpId(terminationPointId)))
                .augmentation(OvsdbTerminationPointAugmentation.class);
    }

    /**
     * Method builds OVS TerminationPoint InstanceIdentifier which is based on
     * SFF name and SFF DataPlane locator name. Method will return valid
     * InstanceIdentifier only if the given SFF and SFF DataPlane locator
     * belongs to SFF instance mapped to OVS.
     *
     * <p>
     *
     * @param ovsdbBridgeNodeId
     *            OVSDB bridge NodeId where the SFF DPL resides
     * @param sffDataPlaneLocatorName
     *            Service Function Forwarder Data Plane locator name
     * @return InstanceIdentifier&lt;TerminationPoint&gt;
     */
    public static InstanceIdentifier<TerminationPoint> buildOvsdbTerminationPointIID(NodeId ovsdbBridgeNodeId,
            String sffDataPlaneLocatorName) {
        return InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(ovsdbBridgeNodeId)))
                .child(TerminationPoint.class, new TerminationPointKey(new TpId(sffDataPlaneLocatorName)));
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static IpAddress convertStringToIpAddress(String ipAddressString) {
        Preconditions.checkNotNull(ipAddressString, "Supplied string value of ipAddress must not be null");

        try {
            return new IpAddress(new Ipv4Address(ipAddressString));
        } catch (RuntimeException e) {
            LOG.debug("Supplied string value of ipAddress ({}) is not an instance of IPv4", ipAddressString, e);
        }

        try {
            return new IpAddress(new Ipv6Address(ipAddressString));
        } catch (RuntimeException e) {
            LOG.debug("Supplied string value of ipAddress ({}) is not an instance of IPv6", ipAddressString, e);
        }

        LOG.error("Supplied string value of ipAddress ({}) cannot be converted to IpAddress object!", ipAddressString);
        return null;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static String convertIpAddressToString(IpAddress ipAddress) {
        Preconditions.checkNotNull(ipAddress, "Supplied IpAddress value must not be null");

        try {
            Preconditions.checkArgument(ipAddress.getIpv4Address().getValue() != null);
            return ipAddress.getIpv4Address().getValue();
        } catch (RuntimeException e) {
            LOG.debug("Supplied IpAddress value ({}) is not an instance of IPv4", ipAddress.toString(), e);
        }

        Preconditions.checkArgument(ipAddress.getIpv6Address().getValue() != null);
        return ipAddress.getIpv6Address().getValue();
    }

    public static boolean putOvsdbTerminationPoints(OvsdbBridgeAugmentation ovsdbBridge,
            List<SffDataPlaneLocator> sffDataPlaneLocatorList) {
        boolean result = true;
        List<OvsdbTerminationPointAugmentation> ovsdbTerminationPointList = SfcSffToOvsMappingAPI
                .buildTerminationPointAugmentationList(sffDataPlaneLocatorList);

        for (OvsdbTerminationPointAugmentation ovsdbTerminationPoint : ovsdbTerminationPointList) {
            boolean partialResult = SfcOvsDataStoreAPI.putOvsdbTerminationPoint(ovsdbBridge, ovsdbTerminationPoint);

            // once result is false, we will keep it false
            // (it will be not overwritten with next partialResults)
            if (result) {
                result = partialResult;
            }
        }

        return result;
    }

    public static ServiceFunctionForwarder augmentSffWithOpenFlowNodeId(ServiceFunctionForwarder sff) {
        return augmentSffWithOpenFlowNodeId(sff, SfcOvsUtil.getOpenFlowNodeIdForSff(sff));
    }

    public static ServiceFunctionForwarder augmentSffWithOpenFlowNodeId(
            ServiceFunctionForwarder sff, String ofNodeId) {

        if (ofNodeId != null) {
            SffOvsBridgeAugmentationBuilder sffOvsBrAugBuilder;
            OvsBridgeBuilder ovsBrBuilder;

            // if augmentation exists, create builders based on existing data
            SffOvsBridgeAugmentation sffOvsBrAug = sff.augmentation(SffOvsBridgeAugmentation.class);
            if (sffOvsBrAug != null) {
                sffOvsBrAugBuilder = new SffOvsBridgeAugmentationBuilder(sffOvsBrAug);

                OvsBridge ovsBridge = sffOvsBrAug.getOvsBridge();
                if (ovsBridge != null) {
                    ovsBrBuilder = new OvsBridgeBuilder(ovsBridge);
                } else {
                    ovsBrBuilder = new OvsBridgeBuilder();
                }

                // if not, create empty builders
            } else {
                sffOvsBrAugBuilder = new SffOvsBridgeAugmentationBuilder();
                ovsBrBuilder = new OvsBridgeBuilder();
            }
            ovsBrBuilder.setOpenflowNodeId(ofNodeId);
            sffOvsBrAugBuilder.setOvsBridge(ovsBrBuilder.build());

            ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder(sff);
            sffBuilder.addAugmentation(SffOvsBridgeAugmentation.class, sffOvsBrAugBuilder.build());
            return sffBuilder.build();
        } else {
            // if the OpenFlowNodeId does not exist, return the original SFF
            return sff;
        }
    }

    /**
     * This gets VxlanDataLocator.
     *
     * @param sff
     *            - Service Function Forwarder
     * @return Ip
     */
    public static Ip getSffVxlanDataLocator(ServiceFunctionForwarder sff) {

        List<SffDataPlaneLocator> dplList = sff.getSffDataPlaneLocator();
        for (SffDataPlaneLocator dpl : dplList) {
            if (dpl.getDataPlaneLocator() != null && dpl.getDataPlaneLocator().getTransport() == VxlanGpe.class) {
                return (Ip) dpl.getDataPlaneLocator().getLocatorType();
            }
        }
        return null;
    }

    /**
     * This gets the OVSDB Manager Topology Node for the
     * {@link ServiceFunctionForwarder}, using the IP address found in an IP
     * based Data Plane Locator. If there isn't an IP based Data Plane Locator,
     * then this will return null.
     *
     * @param serviceFunctionForwarder
     *            - {@link ServiceFunctionForwarder}
     * @return {@link Node}
     */
    public static Node lookupTopologyNode(ServiceFunctionForwarder serviceFunctionForwarder) {
        List<SffDataPlaneLocator> sffDplList = serviceFunctionForwarder.getSffDataPlaneLocator();
        IpAddress ip = null;

        if (sffDplList == null) {
            LOG.debug("No IP Data Plane Locator for Service Function Forwarder {}, ", serviceFunctionForwarder);
            return null;
        }

        /*
         * Go through the Data Plane Locators, looking for an IP-based locator.
         * If we find one, use the IP address from that as the IP for the OVSDB
         * manager connection.
         */
        for (SffDataPlaneLocator sffDpl : sffDplList) {
            if (sffDpl.getDataPlaneLocator() != null && sffDpl.getDataPlaneLocator().getLocatorType() != null) {
                Class<? extends DataContainer> locatorType = sffDpl.getDataPlaneLocator().getLocatorType()
                        .getImplementedInterface();
                if (locatorType.isAssignableFrom(Ip.class)) {
                    Ip ipPortLocator = (Ip) sffDpl.getDataPlaneLocator().getLocatorType();
                    ip = ipPortLocator.getIp();
                }
            }
        }
        if (ip == null) {
            LOG.debug("Could not get IP address for Service Function Forwarder {}", serviceFunctionForwarder);
            return null;
        }
        return SfcOvsUtil.getManagerNodeByIp(ip);

    }

    public static String getOpenFlowNodeIdForSff(ServiceFunctionForwarder serviceFunctionForwarder) {
        NodeId nodeId = getOvsdbAugmentationNodeIdBySff(serviceFunctionForwarder);
        if (nodeId == null) {
            LOG.warn("No NodeId for Service Function Forwarder {}", serviceFunctionForwarder);
            return null;
        }
        DatapathId datapathId = getOvsDataPathId(nodeId);
        if (datapathId == null) {
            LOG.warn("No DatapathId for Service Function Forwarder {}", serviceFunctionForwarder);
            return null;
        }

        return getOpenflowNodeIdFromDpid(datapathId.getValue());
    }

    public static NodeId getOvsdbAugmentationNodeIdBySff(ServiceFunctionForwarder serviceFunctionForwarder) {
        Node managerNode = lookupTopologyNode(serviceFunctionForwarder);
        if (managerNode == null) {
            LOG.warn("No Topology Node for Service Function Forwarder {}", serviceFunctionForwarder);
            return null;
        }

        SffOvsBridgeAugmentation sffOvsBridgeAugmentation = serviceFunctionForwarder
                .augmentation(SffOvsBridgeAugmentation.class);
        if (sffOvsBridgeAugmentation == null) {
            LOG.warn("No SffOvsBridgeAugmentation for Service Function Forwarder {}", serviceFunctionForwarder);
            return null;
        }

        OvsBridge sffOvsBridge = sffOvsBridgeAugmentation.getOvsBridge();
        if (sffOvsBridge == null) {
            LOG.warn("No OvsBridge for SffOvsBridgeAugmentation in Service Function Forwarder {}",
                    serviceFunctionForwarder);
            return null;
        }

        OvsdbBridgeAugmentationBuilder builder = new OvsdbBridgeAugmentationBuilder();
        OvsdbNodeRef ovsdbNodeRef = new OvsdbNodeRef(SfcOvsUtil.buildOvsdbNodeIID(managerNode.getNodeId()));
        builder.setManagedBy(ovsdbNodeRef);
        builder.setBridgeName(new OvsdbBridgeName(sffOvsBridge.getBridgeName()));

        return getOvsBridgeNodeId(builder.build());
    }

    private static DatapathId getOvsDataPathId(NodeId nodeId) {
        OvsdbBridgeAugmentation readBridge = SfcOvsDataStoreAPI.readOvsdbBridge(SfcOvsUtil.buildOvsdbBridgeIID(nodeId));

        if (readBridge == null) {
            LOG.warn("getOvsDataPathId cant readBridge from data store");
            return null;
        }
        return readBridge.getDatapathId();
    }

    public static String getOpenflowNodeIdFromDpid(String dpid) {
        return OPENFLOW + getLongFromDpid(dpid);
    }

    public static Long getLongFromDpid(String dpid) {
        String[] addressInBytes = dpid.split(":");
        return (Long.decode(HEX + addressInBytes[2]) << 40 | Long.decode(HEX + addressInBytes[3]) << 32
                        | Long.decode(HEX + addressInBytes[4]) << 24 | Long.decode(HEX + addressInBytes[5]) << 16
                        | Long.decode(HEX + addressInBytes[6]) << 8 | Long.decode(HEX + addressInBytes[7]));
    }

    public static Node getManagerNodeByIp(IpAddress ip) {
        String ipAddressString = null;

        if (ip == null || ip.getIpv4Address() == null && ip.getIpv6Address() == null) {
            LOG.warn("Invalid IP address");
            return null;
        }
        if (ip.getIpv4Address() != null) {
            ipAddressString = ip.getIpv4Address().getValue();
        } else if (ip.getIpv6Address() != null) {
            ipAddressString = ip.getIpv6Address().getValue();
        }
        Node node = SfcOvsDataStoreAPI.readOvsdbNodeByIp(ipAddressString);

        if (node != null && node.getNodeId() != null) {
            return node;
        } else {
            LOG.warn("OVS Node for IP address {} does not exist!", ipAddressString);
            return null;
        }
    }

    public static OvsdbNodeAugmentation getOvsdbNodeAugmentation(OvsdbNodeRef nodeRef) {
        if (nodeRef.getValue().getTargetType().equals(Node.class)) {
            Node ovsdbNode = SfcOvsDataStoreAPI.readOvsdbNodeByRef(nodeRef);

            if (ovsdbNode != null) {
                return ovsdbNode.augmentation(OvsdbNodeAugmentation.class);
            } else {
                LOG.warn("Could not find ovsdb-node for connection for {}", nodeRef);
            }
        } else {
            LOG.warn("Bridge 'managedBy' non-ovsdb-node.  nodeRef {}", nodeRef);
        }

        return null;
    }

    interface OvsdbTPComp {
        boolean compare(OvsdbTerminationPointAugmentation otp);
    }

    private static Long getOvsPort(String nodeName, OvsdbTPComp comp) {
        if (nodeName == null) {
            return null;
        }

        InstanceIdentifier<Topology> topoIID = buildOvsdbTopologyIID();
        Topology topo = SfcDataStoreAPI.readTransactionAPI(topoIID, LogicalDatastoreType.OPERATIONAL);

        if (topo == null) {
            return null;
        }

        List<Node> nodes = topo.getNode();
        if (nodes == null) {
            return null;
        }

        for (Node node : nodes) {
            OvsdbBridgeAugmentation ovsdbBridgeAugmentation = node.augmentation(OvsdbBridgeAugmentation.class);
            List<TerminationPoint> tpList = node.getTerminationPoint();

            if (ovsdbBridgeAugmentation == null || tpList == null) {
                continue;
            }

            String ofNodeId = getOpenflowNodeIdFromDpid(ovsdbBridgeAugmentation.getDatapathId().getValue());
            if (nodeName.equals(ofNodeId)) {
                for (TerminationPoint tp : tpList) {
                    OvsdbTerminationPointAugmentation otp = tp.augmentation(OvsdbTerminationPointAugmentation.class);
                    if (comp.compare(otp) && otp.getOfport() != null) {
                        return otp.getOfport();
                    }
                }
            }
        }
        return null;
    }

    /**
     * This gets openflow port by port name.
     *
     * @param nodeName
     *            openflow node name
     * @param portName
     *            openflow port name
     * @return port number
     */
    public static Long getOfPortByName(String nodeName, String portName) {
        return getOvsPort(nodeName, otp -> {
            if (otp == null) {
                return false;
            }

            return portName.equals(otp.getName());
        });
    }

    /**
     * This gets vxlan openflow port.
     *
     * @param nodeName
     *            openflow node name
     * @return port number
     */
    public static Long getVxlanOfPort(String nodeName) {
        class VxlanPortCompare implements OvsdbTPComp {
            @Override
            public boolean compare(OvsdbTerminationPointAugmentation otp) {
                if (otp == null) {
                    return false;
                }

                if (otp.getInterfaceType() != InterfaceTypeVxlan.class) {
                    return false;
                }

                List<Options> options = otp.getOptions();
                if (options == null || options.isEmpty()) {
                    return false;
                }

                return options.stream().anyMatch(FLOW_BASED_OPT);

            }
        }

        return getOvsPort(nodeName, new VxlanPortCompare());
    }

    /**
     * This gets the vxlan-gpe openflow port.
     *
     * @param nodeName
     *            openflow node name
     * @return port number
     */
    public static Long getVxlanGpeOfPort(String nodeName) {
        class VxlanGpePortCompare implements OvsdbTPComp {
            @Override
            public boolean compare(OvsdbTerminationPointAugmentation otp) {
                if (otp == null) {
                    return false;
                }

                if (otp.getInterfaceType() != InterfaceTypeVxlan.class) {
                    return false;
                }

                List<Options> options = otp.getOptions();
                if (options == null || options.isEmpty()) {
                    return false;
                }

                return options.stream().filter(FLOW_BASED_OR_GPE_OPT).distinct().count() == 2;
            }
        }

        return getOvsPort(nodeName, new VxlanGpePortCompare());
    }

    /**
     * This gets DPDK Openflow port of the given DPDK port or the first DPDK
     * port in the given Openflow node.
     *
     * @param nodeName
     *            Openflow node name
     * @param dpdkPortName
     *            DPDK port name
     * @return port number if exists, otherwise null
     */
    public static Long getDpdkOfPort(String nodeName, String dpdkPortName) {
        Long dpdkOfPort = null;

        if (nodeName == null) {
            return null;
        }

        String localDpdkPortName = dpdkPortName;
        if (localDpdkPortName  == null) {
            localDpdkPortName = "dpdk0";
        }

        InstanceIdentifier<Topology> topoIID = buildOvsdbTopologyIID();

        Topology topo = SfcDataStoreAPI.readTransactionAPI(topoIID, LogicalDatastoreType.OPERATIONAL);
        if (topo == null) {
            return null;
        }

        List<Node> nodes = topo.getNode();

        if (nodes == null) {
            return null;
        }

        for (Node node : nodes) {
            OvsdbBridgeAugmentation ovsdbBridgeAugmentation = node.augmentation(OvsdbBridgeAugmentation.class);
            if (ovsdbBridgeAugmentation == null) {
                continue;
            }

            String ofNodeId = getOpenflowNodeIdFromDpid(ovsdbBridgeAugmentation.getDatapathId().getValue());
            if (nodeName.equals(ofNodeId)) {
                if (!ovsdbBridgeAugmentation.getDatapathType().equals(DatapathTypeNetdev.class)) {
                    break;
                }

                List<TerminationPoint> tpList = node.getTerminationPoint();
                for (TerminationPoint tp : tpList) {
                    if (tp.getTpId().getValue().equals(localDpdkPortName)) {
                        OvsdbTerminationPointAugmentation otp = tp
                                .augmentation(OvsdbTerminationPointAugmentation.class);
                        if (otp != null && otp.getInterfaceType().equals(InterfaceTypeDpdk.class)) {
                            dpdkOfPort = otp.getOfport();
                        }
                        break;
                    }
                }
            }
        }
        return dpdkOfPort;
    }

    public static ServiceFunctionForwarder findSffByIp(ServiceFunctionForwarders sffs, final IpAddress remoteIp) {
        List<ServiceFunctionForwarder> serviceFunctionForwarders = sffs.getServiceFunctionForwarder();

        if (serviceFunctionForwarders != null && !serviceFunctionForwarders.isEmpty()) {
            for (ServiceFunctionForwarder sff : serviceFunctionForwarders) {
                List<SffDataPlaneLocator> sffDataPlaneLocator = sff.getSffDataPlaneLocator();
                if (sffDataPlaneLocator != null) {
                    for (SffDataPlaneLocator sffLocator : sffDataPlaneLocator) {
                        LocatorType locatorType = sffLocator.getDataPlaneLocator().getLocatorType();
                        if (locatorType instanceof Ip) {
                            Ip ip = (Ip) locatorType;
                            if (ip.getIp().equals(remoteIp)) {
                                return sff;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
