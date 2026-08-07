import { NavLink } from 'react-router-dom'
import {RefreshCw , ShieldAlert , Cpu , Activity , Server } from 'lucide-react'

export default function Sidebar() {
    const services = [
        {
            name: 'Exponential Backoff',
            path: '/exponential-backoff',
            icon: RefreshCw,
            status: 'active',
            port: '8080'
        }
    ]
    return (
        <aside className ="sidebar">
            <div className="sidebar-brand">
                <Cpu className="brand-icon">
                    <div>
                        <h2>Backend Services</h2>
                        <span className="sub-title">Distributed Systems & Backend Design Concepts</span>
                    </div>
                </Cpu>
            </div>

            <nav className="sidebar-nav">
                <div className="nac-section-title">Backend Microservices</div>
                {services.map((s) => {
                    const Icon = s.icon;
                    return (
                        <NavLink
                            key={s.path}
                            to={s.path}
                            className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
                        >
                            <Icon size={18} />
                                <div className="nav-item-content">
                                    <span className="nav-item-title">{s.name}</span>
                                </div>
                            </NavLink>
                    );
                })}
            </nav>
            <div className="sidebar-footer">
                <Server size={16} />
                <span>Made by Parth Bajaj</span>
            </div>
        </aside>
    )
}