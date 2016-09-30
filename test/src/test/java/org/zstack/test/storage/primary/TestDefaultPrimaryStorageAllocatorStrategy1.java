package org.zstack.test.storage.primary;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.simulator.storage.primary.SimulatorPrimaryStorageDetails;
import org.zstack.header.storage.primary.*;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;
public class TestDefaultPrimaryStorageAllocatorStrategy1 {
    CLogger logger = Utils.getLogger(TestDefaultPrimaryStorageAllocatorStrategy1.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    CloudBus bus;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new BeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml")
                .addXml("Simulator.xml")
                .addXml("PrimaryStorageManager.xml")
                .addXml("ZoneManager.xml")
                .addXml("ClusterManager.xml")
                .addXml("HostManager.xml")
                .addXml("ConfigurationManager.xml")
                .addXml("AccountManager.xml")
                .addXml("HostAllocatorManager.xml")
                .build();
        dbf = loader.getComponent(DatabaseFacade.class);
        bus = loader.getComponent(CloudBus.class);
        api = new Api();
        api.startServer();
    }

    @After
    public void tearDown() throws Exception {
        api.stopServer();
    }

    @Test
    public void test() throws ApiSenderException {
        ZoneInventory zone = api.createZones(1).get(0);
        long requiredSize = SizeUnit.GIGABYTE.toByte(10);
        long usedSize = SizeUnit.GIGABYTE.toByte(5);
        SimulatorPrimaryStorageDetails sp = new SimulatorPrimaryStorageDetails();
        sp.setTotalCapacity(SizeUnit.GIGABYTE.toByte(20));
        sp.setAvailableCapacity(sp.getTotalCapacity() - usedSize);
        sp.setUrl("nfs://simulator/primary/");
        sp.setZoneUuid(zone.getUuid());
        PrimaryStorageInventory pinv = api.createSimulatoPrimaryStorage(1, sp).get(0);
        ClusterInventory cluster = api.createClusters(1, zone.getUuid()).get(0);
        HostInventory host = api.createHost(1, cluster.getUuid()).get(0);
        api.attachPrimaryStorage(cluster.getUuid(), pinv.getUuid());
        
        AllocatePrimaryStorageMsg msg = new AllocatePrimaryStorageMsg();
        msg.setRequiredHostUuid(host.getUuid());
        msg.setSize(requiredSize);
        msg.setServiceId(bus.makeLocalServiceId(PrimaryStorageConstant.SERVICE_ID));
        MessageReply reply = bus.call(msg);
        Assert.assertEquals(AllocatePrimaryStorageReply.class, reply.getClass());
        AllocatePrimaryStorageReply ar = (AllocatePrimaryStorageReply) reply;
        Assert.assertEquals(pinv.getUuid(), ar.getPrimaryStorageInventory().getUuid());
        
        PrimaryStorageVO pvo = dbf.findByUuid(pinv.getUuid(), PrimaryStorageVO.class);
        Assert.assertEquals(requiredSize+usedSize, pvo.getCapacity().getTotalCapacity() - pvo.getCapacity().getAvailableCapacity());
    }
}
