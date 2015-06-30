package kc87.web;

import kc87.config.WebAppConfig;
import kc87.config.WebChatProperties;
import kc87.service.SessionService;
import kc87.stubs.StubSessionService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {WebChatProperties.class, WebAppConfig.class})
@WebAppConfiguration
public class LoginControllerTest {

   private LoginController loginController;

   private MockMvc mockMvc;

   @Before
   public void setup() {
      SessionService sessionServiceStub = new StubSessionService();
      this.loginController = new LoginController(sessionServiceStub);
      this.mockMvc = MockMvcBuilders.standaloneSetup(loginController).build();
   }

   @Test
   public void testForm() throws Exception {
      fail("Unimplemented test");
   }

   @Test
   public void testHandleSubmit() throws Exception {
      fail("Unimplemented test");
   }

}