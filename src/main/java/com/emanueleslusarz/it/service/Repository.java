package com.emanueleslusarz.it.service;

import com.emanueleslusarz.it.actor.Studio;

import java.util.ArrayList;
import java.util.List;

public abstract class Repository<T> {

    // Lista contenente gli oggetti per ogni attore
    private List<T> lista;

    // Si usa una lista con array dinamico
    {
        lista = new ArrayList<>();
    }

    public boolean aggiungiAttore(T attore){
        if(!this.lista.contains(attore)){
            this.lista.add(attore);
            return true;
        }
        return false;
    }

    public boolean rimuoviAttore(T attore){
        return this.lista.remove(attore);
    }

}
