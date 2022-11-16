package br.ufsm.csi.redes.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Sonda {

    private String usuario;
    private String tipoMensagem;
    private String status;
}
