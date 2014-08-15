package eu.ammw.msc.plaga.ui;

import eu.ammw.msc.plaga.common.ServiceType;
import eu.ammw.msc.plaga.common.Utils;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;

/**
 * @author AMW
 */
public class UIAgent extends Agent {
	private Logger logger;

	@Override
	public void setup() {
		super.setup();
		logger = Logger.getJADELogger(getClass().getName());
		this.addBehaviour(new Behaviour() {
			private int left = (getArguments() == null) ? 0 : getArguments().length;

			@Override
			public boolean done() {
				return left < 1;
			}

			@Override
			public void action() {
				if (getArguments() == null) {
					left = 0;
					return;
				}
				String path = getArguments()[--left].toString();
				// read file and send to exec
				try {
					byte[] encoded = Base64.encodeBase64(Utils.readFile(path));

					ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
					msg.setContent(new String(encoded));

					// Find EXEC in directory
					AID[] execs = Utils.getAgentsForServiceType(myAgent, ServiceType.EXEC);
					if (execs == null || execs.length == 0) {
						left++;
						logger.info("Found no one to do the task. Will retry in 15s.");
						try {
							Thread.sleep(15000);
						} catch (InterruptedException e1) {
							logger.severe(e1.toString());
							doDelete();
						}
					} else {
						msg.addReceiver(execs[0]);
						msg.addReplyTo(getAID());
						send(msg);
					}
				} catch (IOException ioe) {
					logger.severe("Could not read file: " + path);
					logger.severe(ioe.toString());
				}
			}
		});

		logger.info(getLocalName() + " started.");
	}

	@Override
	public void takeDown() {
		super.takeDown();
		logger.warning(getLocalName() + " terminated!");
	}
}
