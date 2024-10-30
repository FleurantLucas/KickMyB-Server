package org.bbtracker.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.kickmyb.server.ServerApplication;
import org.kickmyb.server.account.BadCredentialsException;
import org.kickmyb.server.account.MUser;
import org.kickmyb.server.account.MUserRepository;
import org.kickmyb.server.account.ServiceAccount;
import org.kickmyb.server.photo.MPhoto;
import org.kickmyb.server.photo.ServicePhoto;
import org.kickmyb.server.task.ServiceTask;
import org.kickmyb.transfer.AddTaskRequest;
import org.kickmyb.transfer.SignupRequest;
import org.kickmyb.transfer.TaskDetailPhotoResponse;
import org.kickmyb.transfer.TaskDetailResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Date;

import static org.assertj.core.api.Fail.fail;
import static org.junit.Assert.assertEquals;

// TODO pour celui ci on aimerait pouvoir mocker l'utilisateur pour ne pas avoir à le créer

// https://reflectoring.io/spring-boot-mock/#:~:text=This%20is%20easily%20done%20by,our%20controller%20can%20use%20it.

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = ServerApplication.class)
@TestPropertySource(locations = "classpath:application-integrationtest.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ServiceTaskTests {

	@Autowired
	private MUserRepository userRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ServiceTask serviceTask;
	@Autowired
	private ServicePhoto servicePhoto;

	@Test
	void testAddTask() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
		MUser u = new MUser();
		u.username = "M. Test";
		u.password = passwordEncoder.encode("Passw0rd!");
		userRepository.saveAndFlush(u);

		AddTaskRequest atr = new AddTaskRequest();
		atr.name = "Tâche de test";
		atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

		serviceTask.addOne(atr, u);

		Assertions.assertEquals(1, serviceTask.home(u.id).size());
	}

	@Test
	void testAddTaskEmpty() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
		MUser u = new MUser();
		u.username = "M. Test";
		u.password = passwordEncoder.encode("Passw0rd!");
		userRepository.saveAndFlush(u);

		AddTaskRequest atr = new AddTaskRequest();
		atr.name = "";
		atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

		try{
			serviceTask.addOne(atr, u);
			fail("Aurait du lancer ServiceTask.Empty");
		} catch (Exception e) {
			assertEquals(ServiceTask.Empty.class, e.getClass());
		}
	}

	@Test
	void testAddTaskTooShort() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
		MUser u = new MUser();
		u.username = "M. Test";
		u.password = passwordEncoder.encode("Passw0rd!");
		userRepository.saveAndFlush(u);

		AddTaskRequest atr = new AddTaskRequest();
		atr.name = "o";
		atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

		try{
			serviceTask.addOne(atr, u);
			fail("Aurait du lancer ServiceTask.TooShort");
		} catch (Exception e) {
			assertEquals(ServiceTask.TooShort.class, e.getClass());
		}
	}

	@Test
	void testAddTaskExisting() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
		MUser u = new MUser();
		u.username = "M. Test";
		u.password = passwordEncoder.encode("Passw0rd!");
		userRepository.saveAndFlush(u);

		AddTaskRequest atr = new AddTaskRequest();
		atr.name = "Bonne tâche";
		atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

		try{
			serviceTask.addOne(atr, u);
			serviceTask.addOne(atr, u);
			fail("Aurait du lancer ServiceTask.Existing");
		} catch (Exception e) {
			assertEquals(ServiceTask.Existing.class, e.getClass());
		}
	}

	@Test
	void testDetails() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing, IOException {
		MUser u = new MUser();
		u.username = "M. Test";
		u.password = passwordEncoder.encode("Passw0rd!");
		userRepository.saveAndFlush(u);

		AddTaskRequest atr = new AddTaskRequest();
		atr.name = "Bonne tâche";
		atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

		serviceTask.addOne(atr, u);
		byte[] simulatedImage = new byte[] {
				(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, // JPEG Header bytes
				(byte) 0xE0, (byte) 0x00, (byte) 0x10, // JFIF header (for illustration)
				(byte) 0x4A, (byte) 0x46, (byte) 0x49, (byte) 0x46, // more JFIF bytes...
				(byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03  // additional bytes
		};

		// Create a MockMultipartFile with the simulated image bytes
		MockMultipartFile mockFile = new MockMultipartFile(
				"file",                    // name of the parameter
				"test-image.jpg",           // original file name
				"image/jpeg",               // content type
				simulatedImage              // file content
		);

		servicePhoto.store(mockFile, 2L);
		TaskDetailPhotoResponse asd = serviceTask.detailPhoto(2L, u);
		System.out.println(asd);
	}

	@Test
	void testRemoveTaskWithPicture() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing, IOException {
		MUser u = new MUser();
		u.username = "M. Test";
		u.password = passwordEncoder.encode("Passw0rd!");
		userRepository.saveAndFlush(u);

		AddTaskRequest atr = new AddTaskRequest();
		atr.name = "Tâche de test";
		atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

		serviceTask.addOne(atr, u);

		byte[] simulatedImage = new byte[] {
				(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, // JPEG Header bytes
				(byte) 0xE0, (byte) 0x00, (byte) 0x10, // JFIF header (for illustration)
				(byte) 0x4A, (byte) 0x46, (byte) 0x49, (byte) 0x46, // more JFIF bytes...
				(byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03  // additional bytes
		};

		// Create a MockMultipartFile with the simulated image bytes
		MockMultipartFile mockFile = new MockMultipartFile(
				"file",                    // name of the parameter
				"test-image.jpg",           // original file name
				"image/jpeg",               // content type
				simulatedImage              // file content
		);

		servicePhoto.store(mockFile, 2L);

		serviceTask.deleteTask(2L, u);

		Assertions.assertEquals(0, serviceTask.home(u.id).size());
	}
}