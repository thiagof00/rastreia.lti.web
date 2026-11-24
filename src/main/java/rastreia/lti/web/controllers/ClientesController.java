package rastreia.lti.web.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import rastreia.lti.web.models.Cliente;
import rastreia.lti.web.repository.ClienteRepository;
import rastreia.lti.web.repository.UsuarioRepository;

@Controller
public class ClientesController {

    ClienteRepository clienteRepository;
    UsuarioRepository usuarioRepository;

    public ClientesController(ClienteRepository clienteRepository, UsuarioRepository usuarioRepository) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/clientes")
    public ModelAndView index() {
        ModelAndView mv = new ModelAndView("clientes/index");
        mv.addObject("totalClientes", clienteRepository.count());
        mv.addObject("clientes", clienteRepository.findAll());
        mv.addObject("paginaAtiva", "clientes");
        return mv;
    }

    @GetMapping("/novo-cliente")
    public ModelAndView novoCliente() {
        ModelAndView mv = new ModelAndView("clientes/formNewCliente");

        mv.addObject("clienteForm", new Cliente());
        mv.addObject("usuarios", usuarioRepository.findAllByClienteIdIsNullAndAtivo(true));
        return mv;
    }

    @PostMapping("/novo-cliente")
    public Object saveCliente(@ModelAttribute("clienteForm") Cliente cliente, RedirectAttributes redirectAttributes) {
        try {
            clienteRepository.save(cliente);
            redirectAttributes.addFlashAttribute("sucesso", "Cliente cadastrado com sucesso!");
            return "redirect:/clientes";
        } catch (Exception e) {
            ModelAndView mv = new ModelAndView("clientes/formNewCliente");
            mv.addObject("paginaAtiva", "clientes");
            mv.addObject("clienteForm", cliente);
            mv.addObject("erro", "Erro ao cadastrar: " + e.getMessage());
            return mv;
        }
    }

}
