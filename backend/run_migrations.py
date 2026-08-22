import sys
import os
from alembic.config import Config
from alembic import command

def run_revision():
    print("Generating migration revision...")
    alembic_cfg = Config("alembic.ini")
    command.revision(alembic_cfg, message="initial schema", autogenerate=True)
    print("Revision generated successfully!")

def run_upgrade():
    print("Applying database migrations...")
    alembic_cfg = Config("alembic.ini")
    command.upgrade(alembic_cfg, "head")
    print("Migrations applied successfully!")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python run_migrations.py [revision|upgrade]")
        sys.exit(1)
        
    action = sys.argv[1]
    try:
        if action == "revision":
            run_revision()
        elif action == "upgrade":
            run_upgrade()
        else:
            print(f"Unknown action: {action}")
            sys.exit(1)
    except Exception as e:
        print(f"Error executing migration action {action}: {e}")
        sys.exit(1)
