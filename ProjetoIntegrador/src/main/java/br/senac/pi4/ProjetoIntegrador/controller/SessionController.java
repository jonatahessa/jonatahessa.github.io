package br.senac.pi4.ProjetoIntegrador.controller;

import br.senac.pi4.ProjetoIntegrador.entity.Cartao;
import br.senac.pi4.ProjetoIntegrador.entity.Cliente;
import br.senac.pi4.ProjetoIntegrador.entity.Endereco;
import br.senac.pi4.ProjetoIntegrador.entity.Imagem;
import br.senac.pi4.ProjetoIntegrador.entity.Pedido;
import br.senac.pi4.ProjetoIntegrador.entity.Produto;
import br.senac.pi4.ProjetoIntegrador.repository.ClienteServiceImpl;
import br.senac.pi4.ProjetoIntegrador.repository.EnderecoServiceImpl;
import br.senac.pi4.ProjetoIntegrador.repository.ImagemServiceImpl;
import br.senac.pi4.ProjetoIntegrador.repository.PedidoServiceImpl;
import br.senac.pi4.ProjetoIntegrador.repository.ProdutoServiceImpl;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import org.apache.catalina.Globals;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/sessao")
@Scope("session")
public class SessionController implements Serializable {

    @Autowired
    ClienteServiceImpl serviceCliente;

    @Autowired
    ProdutoServiceImpl serviceProduto;

    @Autowired
    ImagemServiceImpl serviceImagem;

    @Autowired
    EnderecoServiceImpl serviceEndereco;
    
    @Autowired
    PedidoServiceImpl servicePedido;

    private Set<Produto> carrinho = new HashSet<Produto>();
    private List<Imagem> imagens = new ArrayList<Imagem>();
    private Cartao cartao = new Cartao();
    private int qntCarrinho = 1;
    private Long idEndereco = null;
//    private Cliente cliente = new Cliente();
    private BigDecimal total = new BigDecimal("0.0");

    @RequestMapping("/adicionar/{id}")
    public ModelAndView adicionarProduto(@PathVariable("id") Long idProduto,
            RedirectAttributes redirectAttributes) {
        boolean igual = false;
        for (Produto p : carrinho) {
            if (p.getCodigoProduto() == idProduto) {
                qntCarrinho++;
                igual = true;
            }
        }
        for (Produto p : carrinho) {
            if (p.getCodigoProduto() == idProduto) {
                p.setQntCarrinho(qntCarrinho);
            }
        }

        if (igual == false) {
            Produto p = serviceProduto.obter(idProduto);
            p.setQntCarrinho(1);
//        List<Imagem> temp = serviceImagem.obterCodigoProduto(p.getCodigoProduto());
            carrinho.add(p);
//        imagens.add(temp.get(0));

        }
        return new ModelAndView("redirect:/");
    }

    @RequestMapping("/remover/{id}")
    public ModelAndView removerProduto(@PathVariable("id") Long idProduto,
            RedirectAttributes redirectAttributes, @ModelAttribute("qnt") @Valid int qnt) {

        for (Produto p : carrinho) {
            if (p.getCodigoProduto() == idProduto) {
                int qntAtual = p.getQntCarrinho() - qnt;
                if (qntAtual != 0) {
                    p.setQntCarrinho(qntAtual);
                    break;
                } else {
                    carrinho.remove(p);
                    break;
                }
            }
        }
        return new ModelAndView("redirect:/sessao/carrinho");
    }

    public Set<Produto> getCarrinho() {
        return carrinho;
    }

    public List<Imagem> getImagens() {
        return imagens;
    }

    @RequestMapping(value = "/carrinho", method = RequestMethod.GET)
    public ModelAndView carrinho() {
        BigDecimal tempTotal = new BigDecimal("0.0");
        BigDecimal produto = new BigDecimal("0.0");
        boolean vazio = false;
        if (carrinho.isEmpty()) {
            vazio = true;
        } else {
            for (Produto p : carrinho) {
                produto = p.getPrecoProduto().multiply(new BigDecimal(p.getQntCarrinho()));
                tempTotal = tempTotal.add(produto);
            }
        }
        total = tempTotal;
        total = total.add(new BigDecimal("12.0"));
        return new ModelAndView("clientside/carrinho").addObject("total", total).addObject("vazio", vazio);
    }

    @RequestMapping(value = "/checkoutEndereco")
    public ModelAndView checkoutEndereco() {
        Cliente cliente = serviceCliente.obter(Long.parseLong("5"));
        List<Endereco> enderecos = cliente.getEnderecos();
        return new ModelAndView("clientside/checkoutEndereco").addObject("enderecos", enderecos).addObject("cliente", cliente);
    }

    @RequestMapping(value = "/checkoutPagamento")
    public ModelAndView checkoutPagamento(@ModelAttribute("id") Long codigoEndereco) {
        idEndereco = codigoEndereco;
        return new ModelAndView("clientside/checkoutPagamento").addObject("cartao", new Cartao());
    }

    @RequestMapping(value = "/validaCartao")
    public ModelAndView validaCartao(@ModelAttribute("cartao") @Valid Cartao c,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return new ModelAndView("clientside/checkoutPagamento")
                    .addObject("cartao", c);
        }
        cartao = c;
        return new ModelAndView("redirect:/sessao/checkoutConfirmacao");
    }

    @RequestMapping(value = "/checkoutConfirmacao")
    public ModelAndView checkoutConfirmacao() {
        Endereco endereco = serviceEndereco.obter(idEndereco);
        return new ModelAndView("clientside/checkoutConfirmacao").addObject("endereco", endereco).addObject("cartao", cartao).addObject("total", total);
    }
    
    @RequestMapping(value = "/salvarPedido")
    public ModelAndView salvarPedido() {
        Pedido pedido = new Pedido();
        pedido.setClientePedido(serviceCliente.obter(Long.parseLong("5")));
        pedido.setDataPedido(new Date());
        pedido.setFormaPagamentoPedido("Cartão");
        pedido.setProdutos(carrinho);
        pedido.setStatusPedido("Pedido Recebido!");
        pedido.setUltimaAtualizacao(new Date());
        pedido.setValorPedido(total);
        
        servicePedido.incluir(pedido);

        return new ModelAndView("redirect:/");
    }

}
