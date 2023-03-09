package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CliContext;
import com.logabit.pipeforce.cli.service.ConfigCliService;
import com.logabit.pipeforce.common.model.WorkspaceConfig;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.File;

/**
 * Base class for all tests which needs to access to a real local repository folder.
 * Sets-up the repo folder as temp folder and adds it as config to the
 * cliContext. Deletes the temp folder finally after test has been finished.
 *
 * @author sniederm
 * @since 2.7
 */
public abstract class BaseRepoAwareCliCommandTest {

    @Mock
    protected ConfigCliService configService;

    protected File repoHome;

    @InjectMocks
    protected final CliContext cliContext = new CliContext();

    @Before
    public void setUp() {

        WorkspaceConfig config = new WorkspaceConfig();
        Mockito.when(configService.getWorkspaceConfig()).thenReturn(config);
        this.repoHome = createTestAppRepoHome();
        cliContext.setCurrentWorkDir(repoHome);
    }

    @After
    public void tearDown() {
        FileUtil.delete(repoHome);
    }

    private File createTestAppRepoHome() {

        File testRepo = TestUtil.createTmpFolder("PIPEFORCE_" + getClass().getSimpleName());

        File srcFolder = new File(testRepo, "properties");
        FileUtil.createFolders(srcFolder);

        File pipeforceFolder = new File(testRepo, ".pipeforce");
        FileUtil.createFolders(pipeforceFolder);

        return testRepo;
    }
}
