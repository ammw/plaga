package eu.ammw.msc.plaga.common;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Utility class containing some potentially useful methods.
 *
 * @author AMW
 */
public abstract class Utils {
	public static final String PROPERTY_FILE_LOCATION = "plaga.properties";
	private static Properties properties = null;
	private static Properties defaultProperties;

	static {
		defaultProperties = new Properties();
		defaultProperties.setProperty("exec.downloadDir", "files");
	}

	/**
	 * Registers in DF a service for agent by type.
	 *
	 * @param agent       agent offering service (usually invoked with <code>this</code>)
	 * @param serviceType one of available service types
	 * @return true for success, false otherwise
	 */
	public static boolean registerService(Agent agent, ServiceType serviceType) {
		DFAgentDescription description = new DFAgentDescription();
		description.setName(agent.getAID());
		ServiceDescription service = new ServiceDescription();
		service.setType(serviceType.toString());
		service.setName(agent.getClass().getName() + "_" + agent.getLocalName());
		description.addServices(service);
		try {
			DFService.register(agent, description);
		} catch (FIPAException e) {
			Logger logger = Logger.getJADELogger(agent.getClass().getName());
			logger.severe("Could not register " + serviceType.getDescription() + " agent " + agent.getName());
			logger.severe(e.toString());
			return false;
		}
		return true;
	}

	/**
	 * Unregisters in DF a service for agent.
	 *
	 * @param agent agent offering service (usually invoked with <code>this</code>)
	 * @return true for success, false otherwise
	 */
	public static boolean unregisterService(Agent agent) {
		try {
			DFService.deregister(agent);
		} catch (FIPAException e) {
			Logger logger = Logger.getJADELogger(agent.getClass().getName());
			logger.warning("Could not unregister " + agent.getName());
			logger.warning(e.toString());
		}
		return true;
	}

	/**
	 * Finds in DF all agents registered for given service.
	 *
	 * @param caller      agent calling this method (usually <code>this</code> or <code>myAgent</code>)
	 * @param serviceType requested service type
	 * @return array of agent AIDs; empty when no agents present, <code>null</code> on error
	 */
	public static AID[] getAgentsForServiceType(Agent caller, ServiceType serviceType) {
		ServiceDescription service = new ServiceDescription();
		service.setType(serviceType.toString());
		DFAgentDescription template = new DFAgentDescription();
		template.addServices(service);
		AID[] foundAgents = null;
		try {
			DFAgentDescription[] result = DFService.search(caller, template);
			if (result == null) return null;
			if (result.length == 0) return new AID[0];
			foundAgents = new AID[result.length];
			for (int i = 0; i < result.length; i++)
				foundAgents[i] = result[i].getName();
		} catch (FIPAException e) {
			Logger logger = Logger.getJADELogger(caller.getClass().getName());
			logger.warning(serviceType.getDescription() + " service search unsuccessful");
		}
		return foundAgents;
	}

	/**
	 * Reads content of a file into byte array.
	 *
	 * @param path path of a file to read from
	 * @return content of the whole file, as byte array
	 * @throws IOException
	 */
	public static byte[] readFile(String path) throws IOException {
		Path p = Paths.get(path);
		return Files.readAllBytes(p);
	}

	/**
	 * Creates (or overwrites) a file, writing given binary content to it.
	 *
	 * @param path        path to file
	 * @param fileContent desired content of the new file
	 * @throws IOException
	 */
	public static void writeFile(String path, byte[] fileContent) throws IOException {
		Path p = Paths.get(path);
		Files.write(p, fileContent);
	}

	/**
	 * Creates a directory recursively.
	 *
	 * @param path directory to create
	 * @throws IOException
	 */
	public static void createDirectory(String path) throws IOException {
		Path p = Paths.get(path);
		Files.createDirectories(p);
	}

	/**
	 * Reads given property from default properties file.
	 *
	 * @param key property name
	 * @return property value
	 */
	public static String getProperty(String key) {
		String defaultValue = defaultProperties.getProperty(key);
		if (properties == null) {
			properties = new Properties();
			FileInputStream in = null;
			try {
				in = new FileInputStream(PROPERTY_FILE_LOCATION);
				properties.load(in);
			} catch (IOException ioe) {
				properties = null;
				// TODO use logger
				ioe.printStackTrace();
				return defaultValue;
			} finally {
				try {
					if (in != null)
						in.close();
				} catch (IOException e) {
					// What to do now?
				}
			}
		}
		return properties.getProperty(key, defaultValue);
	}

	/**
	 * Send a reply to say the previous message wasn't recognized properly.
	 *
	 * @param agent   The agent that received the message and wants to reply to it.
	 * @param message The message that caused trouble.
	 */
	public static void informNotUnderstood(Agent agent, ACLMessage message) {
		ACLMessage englishPlease = new ACLMessage(ACLMessage.NOT_UNDERSTOOD);
		englishPlease.setConversationId(message.getConversationId());
		englishPlease.addReceiver(message.getSender());
		agent.send(englishPlease);
	}

	/**
	 * Print exception's stack trace to logger instead ot System.err
	 *
	 * @param throwable The exception we want to have logged.
	 * @param logger    The logger to print out information.
	 */
	public static void logStackTrace(Throwable throwable, Logger logger) {
		logger.severe(throwable.toString());
		StringBuilder builder = new StringBuilder(throwable.getStackTrace().length * 200);
		builder.append("at:\n\t");
		for (StackTraceElement element : throwable.getStackTrace()) {
			builder.append(element.toString());
			builder.append("\n\t");
		}
		logger.severe(builder.toString());
	}
}
