package org.example.game.remote;

import org.example.common.event.ServerStartEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RemoteFacade {

  private ClientService clientService;

  public RemoteFacade(ClientService clientService) {
    this.clientService = clientService;
  }

  @EventListener
  public void serverStart(ServerStartEvent event) throws Exception {
    clientService.testStart();
  }

}
