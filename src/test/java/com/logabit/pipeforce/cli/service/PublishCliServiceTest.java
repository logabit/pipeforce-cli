package com.logabit.pipeforce.cli.service;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@link PublishCliService}.
 *
 * @author sn
 * @since 8.0
 */
public class PublishCliServiceTest {

    @Test
    public void testAddAndRemove() {

        PublishCliService service = new PublishCliService();
        service.add("/Users/max/pipeforce/src/app/myapp/pipeline/foo.pi.yaml", 1l);
        service.add("/Users/max/pipeforce/src/app/myapp/pipeline/bar.pi.yaml", 2l);
        service.add("/Users/max/pipeforce/src/app/anotherApp/pipeline/bar1.pi.yaml", 3l);
        service.add("/Users/max/pipeforce/src/app/anotherApp/pipeline/bar2.pi.yaml", 4l);

        service.remove("/Users/max/pipeforce/src/app/myapp/pipeline/bar.pi.yaml");
        Assert.assertFalse(service.getPublishedMap().containsKey("/Users/max/pipeforce/src/app/myapp/pipeline/bar.pi.yaml"));
        Assert.assertEquals(3, service.getPublishedMap().size());

        service.removeFolder("/Users/max/pipeforce/src/app/anotherApp/");
        Assert.assertEquals(1, service.getPublishedMap().size());
        Assert.assertFalse(service.getPublishedMap().containsKey("/Users/max/pipeforce/src/app/anotherApp/pipeline/bar1.pi.yaml"));
        Assert.assertFalse(service.getPublishedMap().containsKey("/Users/max/pipeforce/src/app/anotherApp/pipeline/bar2.pi.yaml"));
    }
}
