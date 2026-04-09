import React, { useState } from 'react';
import { Home, AlertTriangle, Clock, CreditCard, MapPin, Zap, Droplet } from 'lucide-react';

export default function ResQCustomerPortal() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [category, setCategory] = useState('Electrical');

  return (
    <div className="flex h-screen bg-slate-50 font-sans text-slate-800">
      
      {/* SIDEBAR NAVIGATION */}
      <div className="w-64 bg-slate-900 text-white flex flex-col shadow-2xl z-10">
        <div className="p-6 border-b border-slate-700">
          <h1 className="text-2xl font-extrabold tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-emerald-400">
            RES-Q
          </h1>
          <p className="text-sm text-slate-400 mt-1">Client Portal</p>
        </div>
        
        <nav className="flex-1 p-4 space-y-2">
          <button 
            onClick={() => setActiveTab('dashboard')}
            className={`w-full flex items-center space-x-3 px-4 py-3 rounded-xl transition-all ${activeTab === 'dashboard' ? 'bg-blue-600 shadow-lg shadow-blue-500/30' : 'hover:bg-slate-800 text-slate-300'}`}
          >
            <Home size={20} /> <span>Live Dashboard</span>
          </button>
          <button 
            onClick={() => setActiveTab('request')}
            className={`w-full flex items-center space-x-3 px-4 py-3 rounded-xl transition-all ${activeTab === 'request' ? 'bg-blue-600 shadow-lg shadow-blue-500/30' : 'hover:bg-slate-800 text-slate-300'}`}
          >
            <AlertTriangle size={20} /> <span>Log Emergency</span>
          </button>
          <button className="w-full flex items-center space-x-3 px-4 py-3 rounded-xl hover:bg-slate-800 text-slate-300 transition-all">
            <Clock size={20} /> <span>Past Jobs</span>
          </button>
        </nav>

        <div className="p-6 border-t border-slate-700">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-emerald-400 to-blue-500 flex items-center justify-center text-white font-bold">
              AS
            </div>
            <div>
              <p className="text-sm font-bold">Aarav Sharma</p>
              <p className="text-xs text-slate-400">Zone: North Delhi</p>
            </div>
          </div>
        </div>
      </div>

      {/* MAIN CONTENT AREA */}
      <div className="flex-1 overflow-y-auto bg-slate-50 p-10">
        
        {/* TAB 1: LIVE DASHBOARD */}
        {activeTab === 'dashboard' && (
          <div className="max-w-5xl mx-auto animate-fade-in-up">
            <header className="mb-10">
              <h2 className="text-3xl font-bold text-slate-900">Live Operations Tracker</h2>
              <p className="text-slate-500 mt-2">Monitor your active requests and pending bills.</p>
            </header>

            {/* Glassmorphism Active Job Card */}
            <div className="bg-white rounded-3xl p-8 shadow-xl shadow-slate-200/50 border border-slate-100 relative overflow-hidden">
              <div className="absolute top-0 left-0 w-2 h-full bg-emerald-500"></div>
              
              <div className="flex justify-between items-start mb-6">
                <div>
                  <span className="inline-block px-3 py-1 bg-emerald-100 text-emerald-700 rounded-full text-xs font-bold uppercase tracking-wider mb-3">
                    Status: En Route
                  </span>
                  <h3 className="text-2xl font-bold text-slate-800">Sparks in Main Switchboard</h3>
                  <p className="text-slate-500 flex items-center mt-2">
                    <MapPin size={16} className="mr-1" /> Exact Loc: 2nd Floor Master Bedroom
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-sm text-slate-400 font-medium uppercase">Req ID</p>
                  <p className="text-2xl font-black text-slate-800">#1042</p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-6 p-6 bg-slate-50 rounded-2xl border border-slate-100">
                <div>
                  <p className="text-sm text-slate-500 mb-1">Assigned Technician</p>
                  <p className="font-bold text-lg text-slate-900 flex items-center">
                    Rajesh Kumar <span className="ml-2 text-yellow-500">★ 4.8</span>
                  </p>
                </div>
                <div>
                  <p className="text-sm text-slate-500 mb-1">Estimated Arrival</p>
                  <p className="font-bold text-lg text-blue-600">15 - 20 Minutes</p>
                </div>
              </div>

              {/* Pending Bill Section */}
              <div className="mt-8 flex items-center justify-between border-t border-slate-100 pt-6">
                <div>
                  <p className="text-sm text-slate-500">Pending Invoice Amount</p>
                  <p className="text-3xl font-black text-slate-900">₹820.00</p>
                </div>
                <button className="bg-slate-900 hover:bg-slate-800 text-white px-8 py-4 rounded-xl font-bold transition-all shadow-lg flex items-center">
                  <CreditCard size={20} className="mr-2" /> Pay & Complete Job
                </button>
              </div>
            </div>
          </div>
        )}

        {/* TAB 2: LOG EMERGENCY */}
        {activeTab === 'request' && (
          <div className="max-w-3xl mx-auto animate-fade-in-up">
            <header className="mb-10">
              <h2 className="text-3xl font-bold text-slate-900">Request Immediate Help</h2>
              <p className="text-slate-500 mt-2">Dispatch a certified technician to your location instantly.</p>
            </header>

            <div className="bg-white rounded-3xl p-10 shadow-xl shadow-slate-200/50 border border-slate-100">
              
              <div className="mb-8">
                <label className="block text-sm font-bold text-slate-700 mb-4">Select Emergency Category</label>
                <div className="grid grid-cols-2 gap-4">
                  <div 
                    onClick={() => setCategory('Electrical')}
                    className={`cursor-pointer p-6 rounded-2xl border-2 flex flex-col items-center justify-center transition-all ${category === 'Electrical' ? 'border-blue-500 bg-blue-50 text-blue-700' : 'border-slate-200 hover:border-blue-300 text-slate-500'}`}
                  >
                    <Zap size={32} className="mb-3" />
                    <span className="font-bold">Electrical</span>
                  </div>
                  <div 
                    onClick={() => setCategory('Plumbing')}
                    className={`cursor-pointer p-6 rounded-2xl border-2 flex flex-col items-center justify-center transition-all ${category === 'Plumbing' ? 'border-blue-500 bg-blue-50 text-blue-700' : 'border-slate-200 hover:border-blue-300 text-slate-500'}`}
                  >
                    <Droplet size={32} className="mb-3" />
                    <span className="font-bold">Plumbing</span>
                  </div>
                </div>
              </div>

              <div className="space-y-6">
                <div>
                  <label className="block text-sm font-bold text-slate-700 mb-2">Describe the Issue</label>
                  <input 
                    type="text" 
                    placeholder="e.g., Sparks coming from the main AC switchboard..." 
                    className="w-full px-5 py-4 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all"
                  />
                </div>
                <div>
                  <label className="block text-sm font-bold text-slate-700 mb-2">Exact Location</label>
                  <div className="relative">
                    <MapPin size={20} className="absolute left-4 top-4 text-slate-400" />
                    <input 
                      type="text" 
                      placeholder="e.g., 2nd Floor, Guest Bedroom" 
                      className="w-full pl-12 pr-5 py-4 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all"
                    />
                  </div>
                </div>
                <button className="w-full bg-red-500 hover:bg-red-600 text-white font-bold text-lg py-5 rounded-xl mt-6 shadow-lg shadow-red-500/30 transition-all flex justify-center items-center">
                  <AlertTriangle size={24} className="mr-2" /> Dispatch Technician Now
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}